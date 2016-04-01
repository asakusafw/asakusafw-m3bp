/**
 * Copyright 2011-2016 Asakusa Framework Team.
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
package com.asakusafw.m3bp.client;

import static com.asakusafw.m3bp.client.Constants.*;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.bridge.broker.ResourceBroker;
import com.asakusafw.bridge.broker.ResourceSession;
import com.asakusafw.bridge.stage.StageInfo;
import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.m3bp.mirror.ConfigurationMirror;
import com.asakusafw.m3bp.mirror.ConfigurationMirror.AffinityMode;
import com.asakusafw.m3bp.mirror.ConfigurationMirror.BufferAccessMode;
import com.asakusafw.m3bp.mirror.EngineMirror;
import com.asakusafw.m3bp.mirror.jni.EngineMirrorImpl;
import com.asakusafw.m3bp.mirror.mock.MockEngineMirror;
import com.asakusafw.runtime.core.HadoopConfiguration;
import com.asakusafw.runtime.core.ResourceConfiguration;
import com.asakusafw.runtime.core.context.RuntimeContext;

/**
 * Executes {@link GraphInfo} using M3BP.
 */
public final class GraphExecutor {

    static final Logger LOG = LoggerFactory.getLogger(GraphExecutor.class);

    private GraphExecutor() {
        return;
    }

    /**
     * Extracts {@link GraphInfo} from the specified class.
     * @param entry the target class
     * @return the loaded graph
     * @throws IllegalStateException if failed to extract DAG from the target class
     */
    public static GraphInfo extract(Class<?> entry) {
        Arguments.requireNonNull(entry);
        try {
            Object object = entry.newInstance();
            Invariants.require(object instanceof Supplier<?>, () -> MessageFormat.format(
                    "entry class must be a Supplier: {0}",
                    object.getClass().getName()));
            Object info = ((Supplier<?>) object).get();
            Invariants.require(info instanceof GraphInfo, () -> MessageFormat.format(
                    "entry class must supply GraphInfo: {0}",
                    object.getClass().getName(),
                    Optionals.of(info).map(Object::getClass).map(Class::getName).orElse(null)));
            return (GraphInfo) info;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(MessageFormat.format(
                    "exception occurred while loading DAG: {0}",
                    entry), e);
        }
    }

    /**
     * Executes DAG.
     * @param context the current processor context
     * @param graph the target DAG
     * @throws IOException if I/O error was occurred while executing
     * @throws InterruptedException if interrupted while executing
     */
    public static void execute(ProcessorContext context, GraphInfo graph) throws IOException, InterruptedException {
        Arguments.requireNonNull(context);
        Arguments.requireNonNull(graph);
        String libraryPath = context.getProperty(KEY_NATIVE_LIBRARY).orElse(NATIVE_LIBRARY_PATH);
        try (NativeLibraryHolder holder = NativeLibraryHolder.extract(context.getClassLoader(), libraryPath);
                ResourceSession session = ResourceBroker.attach(ResourceBroker.Scope.VM, s -> configure(s, context));
                EngineMirror engine = newEngine(context, holder.getFile())) {
            engine.getGraph().drive(graph);
            configure(engine.getConfiguration(), context);
            if (RuntimeContext.get().isSimulation() == false) {
                engine.run(context);
            }
        }
    }

    private static EngineMirror newEngine(ProcessorContext context, File nativeLibrary) {
        Capability mock = parseEnum(Capability.class, context, KEY_ENGINE_MOCK).orElse(Capability.NEVER);
        if (mock == Capability.ALWAYS) {
            return new MockEngineMirror(nativeLibrary);
        }
        try {
            EngineMirrorImpl engine = new EngineMirrorImpl(nativeLibrary);
            LOG.debug("native engine implementaiton is available"); //$NON-NLS-1$
            return engine;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            if (mock == Capability.POSSIBLE) {
                LOG.info("trying mock engine implementation"); //$NON-NLS-1$
                try {
                    return new MockEngineMirror(nativeLibrary);
                } catch (UnsatisfiedLinkError | NoClassDefFoundError inner) {
                    e.addSuppressed(inner);
                }
            }
            throw e;
        }
    }

    private static void configure(ConfigurationMirror configuration, ProcessorContext context) {
        configureInt(configuration::withMaxConcurrency, context, KEY_THREAD_MAX);
        configureInt(configuration::withPartitionCount, context, KEY_PARTITIONS);
        configureLong(configuration::withOutputBufferSize, context, KEY_OUTPUT_BUFFER_SIZE);
        configureLong(configuration::withOutputRecordsPerBuffer, context, KEY_OUTPUT_BUFFER_RECORDS);
        configureEnum(configuration::withAffinityMode, AffinityMode.class, context, KEY_THREAD_AFFINITY);
        configureEnum(configuration::withBufferAccessMode, BufferAccessMode.class, context, KEY_BUFFER_ACCESS);
        configureFile(configuration::withProfilingOutput, context, KEY_PROFILE_OUTPUT);
        if (LOG.isDebugEnabled()) {
            LOG.debug(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                    KEY_THREAD_MAX, configuration.getMaxConcurrency()));
            LOG.debug(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                    KEY_PARTITIONS, configuration.getPartitionCount()));
            LOG.debug(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                    KEY_OUTPUT_BUFFER_SIZE, configuration.getOutputBufferSize()));
            LOG.debug(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                    KEY_OUTPUT_BUFFER_RECORDS, configuration.getOutputRecordsPerBuffer()));
            LOG.debug(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                    KEY_THREAD_AFFINITY, configuration.getAffinityMode()));
            LOG.debug(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                    KEY_BUFFER_ACCESS, configuration.getBufferAccessMode()));
            LOG.debug(MessageFormat.format("{0}: {1}", //$NON-NLS-1$
                    KEY_PROFILE_OUTPUT, configuration.getProfilingOutput()));
        }
    }

    private static void configure(ResourceSession session, ProcessorContext context) {
        session.put(StageInfo.class,
                context.getResource(StageInfo.class).get());
        session.put(ResourceConfiguration.class,
                new HadoopConfiguration(context.getResource(Configuration.class).get()));
    }

    private static void configureInt(IntConsumer target, ProcessorContext context, String key) {
        context.getProperty(key)
                .map(value -> Arguments.safe(() -> Integer.parseInt(value), () -> MessageFormat.format(
                        "{0} must be an integer: {1}",
                        key, value)))
                .ifPresent(target::accept);
    }

    private static void configureLong(LongConsumer target, ProcessorContext context, String key) {
        context.getProperty(key)
                .map(value -> Arguments.safe(() -> Long.parseLong(value), () -> MessageFormat.format(
                        "{0} must be an integer: {1}",
                        key, value)))
                .ifPresent(target::accept);
    }

    private static <T extends Enum<T>> Optional<T> parseEnum(Class<T> type, ProcessorContext context, String key) {
        return context.getProperty(key)
                .map(value -> value.toUpperCase(Locale.ENGLISH))
                .map(value -> Arguments.safe(() -> Enum.valueOf(type, value), () -> MessageFormat.format(
                        "{0} must be one of [{2}]: {1}",
                        key, value,
                        Stream.of(type.getEnumConstants())
                            .map(v -> v.name().toLowerCase(Locale.ENGLISH))
                            .collect(Collectors.joining(", ")))));
    }

    private static void configureFile(Consumer<File> target, ProcessorContext context, String key) {
        context.getProperty(key)
                .map(File::new)
                .ifPresent(target::accept);
    }

    private static <T extends Enum<T>> void configureEnum(
            Consumer<? super T> target,
            Class<T> type, ProcessorContext context, String key) {
        parseEnum(type, context, key).ifPresent(target::accept);
    }
}
