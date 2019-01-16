/**
 * Copyright 2011-2019 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.m3bp.mirror.jni;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToIntFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Invariants;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.m3bp.bridge.VertexProcessorBridge;
import com.asakusafw.m3bp.mirror.ConfigurationMirror;
import com.asakusafw.m3bp.mirror.ConfigurationMirror.BufferAccessMode;
import com.asakusafw.m3bp.mirror.EngineMirror;
import com.asakusafw.m3bp.mirror.FlowGraphMirror;
import com.asakusafw.m3bp.mirror.TaskMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.m3bp.mirror.unsafe.UnsafeUtil;

/**
 * JNI bridge of {@link EngineMirror}.
 * @since 0.1.0
 * @version 0.2.1
 */
public class EngineMirrorImpl implements EngineMirror, NativeMirror {

    static final Logger LOG = LoggerFactory.getLogger(EngineMirrorImpl.class);

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private static final String LIBRARY_NAME = "m3bpjni"; //$NON-NLS-1$

    private static final String KEY_LIBRARY_PATH = "com.asakusafw.m3bp.library.path"; //$NON-NLS-1$

    private static final String KEY_LOGGER_LEVEL = "com.asakusafw.m3bp.core.log.level"; //$NON-NLS-1$

    private static final String NAME_LOGGER = "com.asakusafw.m3bp.core"; //$NON-NLS-1$

    static {
        File file = Optionals.of(System.getProperty(KEY_LIBRARY_PATH))
                .map(String::trim)
                .filter(s -> s.isEmpty() == false)
                .map(File::new)
                .map(f -> f.isDirectory() ? new File(f, System.mapLibraryName(LIBRARY_NAME)) : f)
                .orElse(null);
        if (file == null) {
            LOG.debug("loading library: {}", LIBRARY_NAME); //$NON-NLS-1$
            System.loadLibrary(LIBRARY_NAME);
        } else {
            LOG.debug("loading library: {}", file.getAbsolutePath()); //$NON-NLS-1$
            System.load(file.getAbsolutePath());
        }
        EngineLogLevel logLevel = Optionals.of(System.getProperty(KEY_LOGGER_LEVEL))
                .map(String::trim)
                .filter(s -> s.isEmpty() == false)
                .flatMap(value -> {
                    try {
                        return Optionals.of(EngineLogLevel.valueOf(value.toUpperCase(Locale.ENGLISH)));
                    } catch (NoSuchElementException e) {
                        LOG.warn(MessageFormat.format(
                                "invalid log level: {0}={1}",
                                KEY_LOGGER_LEVEL, value), e);
                        return Optionals.empty();
                    }
                })
                .orElseGet(() -> EngineLogLevel.get(NAME_LOGGER));
        initializeNativeLogger(logLevel);
    }

    private final Pointer reference;

    private final ConfigurationMirror configuration;

    private final FlowGraphMirrorImpl graph;

    private final AtomicReference<ProcessorContext> runningContext = new AtomicReference<>(null);

    private final ConcurrentMap<Pointer, VertexProcessorBridge> runningBridges = new ConcurrentHashMap<>();

    private final ThreadLocal<ThreadState> threadState = ThreadLocal.withInitial(() -> ThreadState.UNMANAGED);

    /**
     * Creates a new instance.
     * @param library the application native library file (optional)
     */
    public EngineMirrorImpl(File library) {
        this.reference = new Pointer(initialize0(Optionals.of(library)
                .map(f -> f.getAbsolutePath())
                .orElse(null)));
        this.configuration = new ConfigurationMirrorImpl(new Pointer(getConfiguration0(reference.getAddress())));
        this.graph = new FlowGraphMirrorImpl(new Pointer(getGraph0(reference.getAddress())));
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    public ConfigurationMirror getConfiguration() {
        return configuration;
    }


    @Override
    public FlowGraphMirror getGraph() {
        return graph;
    }

    @Override
    public void run(ProcessorContext context) throws IOException, InterruptedException {
        Arguments.requireNonNull(context);
        validateConfigurations();
        if (runningContext.compareAndSet(null, context) == false) {
            throw new IllegalStateException(MessageFormat.format(
                    "other process is running: {0}", //$NON-NLS-1$
                    context));
        }
        threadState.set(ThreadState.MAIN);
        try {
            runningBridges.clear();
            run0(getPointer().getAddress());
        } finally {
            runningBridges.clear();
            runningContext.set(null);
            threadState.set(ThreadState.UNMANAGED);
        }
    }

    private void validateConfigurations() {
        ConfigurationMirror.BufferAccessMode mode = getConfiguration().getBufferAccessMode();
        if (mode == BufferAccessMode.UNSAFE && UnsafeUtil.isAvailable() == false) {
            throw new UnsupportedOperationException("unsafe facilities are not available in this environment");
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        close0(getPointer().getAddress());
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "EngineMirror[{0}]", //$NON-NLS-1$
                getPointer());
    }

    /**
     * Initializes the current worker thread.
     * @throws IOException if I/O error was occurred during this operation
     * @throws InterruptedException if interrupted during this operation
     * @since 0.2.1
     */
    public void doThreadInitialize() throws IOException, InterruptedException {
        if (threadState.get() != ThreadState.UNMANAGED) {
            return;
        }
        Thread thread = Thread.currentThread();
        ProcessorContext context = runningContext.get();
        if (context == null) {
            LOG.warn("current m3bp context has been already detached");
            return;
        }
        String name = String.format("m3bp-worker-%d", THREAD_COUNTER.incrementAndGet());
        if (LOG.isDebugEnabled()) {
            LOG.debug("initialiing m3bp worker thread: {}", name);
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    thread.setName(name);
                    if (thread.getContextClassLoader() == null) {
                        thread.setContextClassLoader(context.getClassLoader());
                    }
                } catch (SecurityException e) {
                    LOG.warn("error occurred while initializing worker thread: {}", thread, e);
                }
                return null;
            }
        });
        threadState.set(ThreadState.WORKER);
    }

    /**
     * Initializes the current worker thread.
     * @throws IOException if I/O error was occurred during this operation
     * @throws InterruptedException if interrupted during this operation
     * @since 0.2.1
     */
    public void doThreadFinalize() throws IOException, InterruptedException {
        if (threadState.get() != ThreadState.WORKER) {
            return;
        }
        Thread thread = Thread.currentThread();
        ProcessorContext context = runningContext.get();
        if (context == null) {
            LOG.warn("current m3bp context has been already detached");
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("finalizing m3bp worker thread: {}", Thread.currentThread().getName());
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    if (thread.getContextClassLoader() == context.getClassLoader()) {
                        thread.setContextClassLoader(null);
                    }
                } catch (SecurityException e) {
                    LOG.warn("error occurred while finalizing worker thread: {}", thread, e);
                }
                return null;
            }
        });
        threadState.set(ThreadState.UNMANAGED);
    }

    /**
     * Invokes {@link VertexProcessorBridge#globalInitialize(ProcessorContext, TaskMirror)} using the current context.
     * @param vertexReference the vertex mirror reference
     * @param taskReference the task mirror reference
     * @throws IOException if I/O error was occurred during this operation
     * @throws InterruptedException if interrupted during this operation
     */
    public void doGlobalInitialize(long vertexReference, long taskReference) throws IOException, InterruptedException {
        doCall(vertexReference, taskReference, true, VertexProcessorBridge::globalInitialize);
    }

    /**
     * Invokes {@link VertexProcessorBridge#globalFinalize(ProcessorContext, TaskMirror)} using the current context.
     * @param vertexReference the vertex mirror reference
     * @param taskReference the task mirror reference
     * @throws IOException if I/O error was occurred during this operation
     * @throws InterruptedException if interrupted during this operation
     */
    public void doGlobalFinalize(long vertexReference, long taskReference) throws IOException, InterruptedException {
        VertexProcessorBridge bridge = doCall(vertexReference, taskReference, false,
                VertexProcessorBridge::globalFinalize);
        runningBridges.remove(new Pointer(vertexReference), bridge);
    }

    /**
     * Invokes {@link VertexProcessorBridge#threadLocalInitialize(ProcessorContext, TaskMirror)}
     * using the current context.
     * @param vertexReference the vertex mirror reference
     * @param taskReference the task mirror reference
     * @throws IOException if I/O error was occurred during this operation
     * @throws InterruptedException if interrupted during this operation
     */
    public void doLocalInitialize(long vertexReference, long taskReference) throws IOException, InterruptedException {
        doCall(vertexReference, taskReference, false, VertexProcessorBridge::threadLocalInitialize);
    }

    /**
     * Invokes {@link VertexProcessorBridge#threadLocalFinalize(ProcessorContext, TaskMirror)}
     * using the current context.
     * @param vertexReference the vertex mirror reference
     * @param taskReference the task mirror reference
     * @throws IOException if I/O error was occurred during this operation
     * @throws InterruptedException if interrupted during this operation
     */
    public void doLocalFinalize(long vertexReference, long taskReference) throws IOException, InterruptedException {
        doCall(vertexReference, taskReference, false, VertexProcessorBridge::threadLocalFinalize);
    }

    /**
     * Invokes {@link VertexProcessorBridge#run(ProcessorContext, TaskMirror)} using the current context.
     * @param vertexReference the vertex mirror reference
     * @param taskReference the task mirror reference
     * @throws IOException if I/O error was occurred during this operation
     * @throws InterruptedException if interrupted during this operation
     */
    public void doRun(long vertexReference, long taskReference) throws IOException, InterruptedException {
        doCall(vertexReference, taskReference, false, VertexProcessorBridge::run);
    }

    /**
     * Invokes {@link VertexProcessorBridge#taskCount()} using the current context.
     * @param vertexReference the vertex mirror reference
     * @return the result value
     */
    public int doTaskCount(long vertexReference) {
        return doGet(vertexReference, VertexProcessorBridge::taskCount);
    }

    /**
     * Invokes {@link VertexProcessorBridge#maxConcurrency()} using the current context.
     * @param vertexReference the vertex mirror reference
     * @return the result value
     */
    public int doMaxConcurrency(long vertexReference) {
        return doGet(vertexReference, VertexProcessorBridge::maxConcurrency);
    }

    VertexProcessorBridge getBridge(Pointer vertexMirror, boolean initial) {
        VertexMirror vertex = toVertexMirror(vertexMirror);
        if (initial) {
            VertexProcessorBridge bridge = new VertexProcessorBridge(vertex);
            if (runningBridges.putIfAbsent(vertexMirror, bridge) != null) {
                throw new IllegalStateException(MessageFormat.format(
                        "reinitialized vertex processor: {0}", //$NON-NLS-1$
                        vertex.getName()));
            }
            return bridge;
        } else {
            VertexProcessorBridge bridge = runningBridges.get(vertexMirror);
            Invariants.requireNonNull(bridge, () -> MessageFormat.format(
                        "vertex processor has never been initialized: {0}", //$NON-NLS-1$
                        vertex.getName()));
            return bridge;
        }
    }

    private VertexMirror toVertexMirror(Pointer pointer) {
        for (VertexMirror v : graph.getVertices()) {
            assert v instanceof NativeMirror;
            if (((NativeMirror) v).getPointer().equals(pointer)) {
                return v;
            }
        }
        throw new IllegalStateException(MessageFormat.format(
                "missing vertex mirror: {0}", //$NON-NLS-1$
                pointer));
    }

    private VertexProcessorBridge doCall(
            long vertexReference,
            long taskReference,
            boolean initialize,
            Callback callback) throws IOException, InterruptedException {
        ProcessorContext context = runningContext.get();
        assert context != null;
        TaskMirrorImpl task = new TaskMirrorImpl(new Pointer(taskReference), configuration);
        VertexProcessorBridge bridge = getBridge(new Pointer(vertexReference), initialize);
        try {
            callback.call(bridge, context, task);
        } catch (Throwable t) {
            LOG.error("exception was occurred in vertex", t);
            throw t;
        }
        return bridge;
    }

    private int doGet(long vertexReference, ToIntFunction<VertexProcessorBridge> func) {
        VertexProcessorBridge bridge = getBridge(new Pointer(vertexReference), false);
        try {
            return func.applyAsInt(bridge);
        } catch (Throwable t) {
            LOG.error("exception was occurred in vertex", t);
            throw t;
        }
    }

    private static void initializeNativeLogger(EngineLogLevel logLevel) {
        Arguments.requireNonNull(logLevel);
        LOG.debug("initialize engine logger: {}", logLevel);
        initializeNativeLogger0(logLevel.id);
    }

    @FunctionalInterface
    private interface Callback {

        void call(VertexProcessorBridge bridge,
                ProcessorContext context, TaskMirror task) throws IOException, InterruptedException;
    }

    private native long initialize0(String library);

    private static native long getConfiguration0(long self);

    private static native long getGraph0(long self);

    private static native void run0(long self);

    private static native void close0(long self);

    private static native void initializeNativeLogger0(int id);

    private enum ThreadState {
        UNMANAGED,
        MAIN,
        WORKER,
    }
}
