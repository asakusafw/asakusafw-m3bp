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
package com.asakusafw.m3bp.bridge;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.processor.EdgeIoProcessorContext;
import com.asakusafw.dag.api.processor.EdgeReader;
import com.asakusafw.dag.api.processor.EdgeWriter;
import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.api.processor.TaskInfo;
import com.asakusafw.dag.api.processor.TaskProcessor;
import com.asakusafw.dag.api.processor.TaskProcessorContext;
import com.asakusafw.dag.api.processor.TaskSchedule;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.api.processor.VertexProcessorContext;
import com.asakusafw.dag.api.processor.basic.ForwardProcessorContext;
import com.asakusafw.dag.api.processor.extension.ProcessorContextDecorator;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.InterruptibleIo;
import com.asakusafw.lang.utils.common.Invariants;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.TaskMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * Bridge implementation of M3BP {@code ProcessorBase}.
 */
public class VertexProcessorBridge {

    static final Logger LOG = LoggerFactory.getLogger(VertexProcessorBridge.class);

    private final VertexMirror vertexMirror;

    private final AtomicReference<Driver> driver = new AtomicReference<>();

    /**
     * Creates a new instance.
     * @param vertexMirror the vertex mirror
     */
    public VertexProcessorBridge(VertexMirror vertexMirror) {
        Arguments.requireNonNull(vertexMirror);
        this.vertexMirror = vertexMirror;
    }

    /**
     * Initializes globally.
     * @param context the current context
     * @param task the task info
     * @throws IOException if I/O error was occurred while processing
     * @throws InterruptedException if interrupted while processing
     */
    public void globalInitialize(
            ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
        if (LOG.isTraceEnabled()) {
            LOG.trace(MessageFormat.format(
                    "global_initialize: {0}:{1}", //$NON-NLS-1$
                    vertexMirror,
                    task));
        }
        Driver d = new Driver(context, vertexMirror, task);
        if (driver.compareAndSet(null, d) == false) {
            LOG.warn(MessageFormat.format(
                    "multiple initialization: {0}",
                    vertexMirror.getName()));
        }
    }

    /**
     * Finalizes globally.
     * @param context the current context
     * @param task the task info
     * @throws IOException if I/O error was occurred while processing
     * @throws InterruptedException if interrupted while processing
     */
    public void globalFinalize(
            ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
        if (LOG.isTraceEnabled()) {
            LOG.trace(MessageFormat.format(
                    "global_finalize: {0}:{1} ({2})", //$NON-NLS-1$
                    vertexMirror,
                    driver,
                    task));
        }
        try (Driver d = driver.getAndSet(null)) {
            if (d == null) {
                LOG.warn(MessageFormat.format(
                        "multiple finalization: {0}",
                        vertexMirror.getName()));
            }
        }
    }

    /**
     * Initializes locally.
     * @param context the current context
     * @param task the task info
     * @throws IOException if I/O error was occurred while processing
     * @throws InterruptedException if interrupted while processing
     */
    public void threadLocalInitialize(
            ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
        if (LOG.isTraceEnabled()) {
            LOG.trace(MessageFormat.format(
                    "thread_local_initialize: {0}:{1} ({2})", //$NON-NLS-1$
                    vertexMirror,
                    driver,
                    task));
        }
        get().threadLocalInitialize(context, task);
    }

    /**
     * Finalizes locally.
     * @param context the current context
     * @param task the task info
     * @throws IOException if I/O error was occurred while processing
     * @throws InterruptedException if interrupted while processing
     */
    public void threadLocalFinalize(
            ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
        if (LOG.isTraceEnabled()) {
            LOG.trace(MessageFormat.format(
                    "thread_local_finalize: {0}:{1} ({2})", //$NON-NLS-1$
                    vertexMirror,
                    driver,
                    task));
        }
        get().threadLocalFinalize(context, task);
    }

    /**
     * Runs a task.
     * @param context the current context
     * @param task the task info
     * @throws IOException if I/O error was occurred while processing
     * @throws InterruptedException if interrupted while processing
     */
    public void run(ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
        if (LOG.isTraceEnabled()) {
            LOG.trace(MessageFormat.format(
                    "run: {0}:{1} ({2})", //$NON-NLS-1$
                    vertexMirror,
                    driver,
                    task));
        }
        get().run(context, task);
    }

    /**
     * Returns the computed task count.
     * This method is only available after {@link #globalInitialize(ProcessorContext, TaskMirror)} was completed.
     * @return the computed task count, or {@code -1} if it is not defined
     */
    public int taskCount() {
        return get().taskCount();
    }

    /**
     * Returns the number of max concurrency.
     * This method is only available after {@link #globalInitialize(ProcessorContext, TaskMirror)} was completed.
     * @return the number of max concurrency, or {@code -1} if it is not defined
     */
    public int maxConcurrency() {
        return get().maxConcurrency();
    }

    private Driver get() {
        Driver d = driver.get();
        if (d == null) {
            throw new IllegalStateException(MessageFormat.format(
                    "missing driver: {0}",
                    vertexMirror.getName()));
        }
        return d;
    }

    private static final class Driver implements InterruptibleIo {

        private final VertexMirror vertexMirror;

        private final VertexProcessor vertexProcessor;

        private final String vertexLabel;

        private final ThreadLocal<TaskProcessor> taskProcessor = new ThreadLocal<>();

        private final Optional<Queue<TaskInfo>> taskQueue;

        private final int numberOfTasks;

        private final ProcessorContextDecorator decorator;

        Driver(
                ProcessorContext context,
                VertexMirror vertex, TaskMirror task) throws IOException, InterruptedException {
            this.vertexMirror = vertex;
            this.decorator = context.getResource(ProcessorContextDecorator.class)
                    .orElse(ProcessorContextDecorator.NULL);
            VertexProcessorContext c = decorator.bless(new VertexContext(context, vertexMirror, task));
            this.vertexProcessor = vertexMirror.newProcessor(c.getClassLoader());
            this.vertexLabel = vertexProcessor.toString();
            if (LOG.isDebugEnabled()) {
                LOG.debug(MessageFormat.format(
                        "start vertex: {0} ({1})",
                        vertexMirror.getName(),
                        vertexLabel));
            }
            this.taskQueue = doInitialize(c, vertexProcessor);
            this.numberOfTasks = taskQueue.map(Collection::size).orElse(-1);
        }

        private static Optional<Queue<TaskInfo>> doInitialize(
                VertexProcessorContext context, VertexProcessor processor) throws IOException, InterruptedException {
            Optional<? extends TaskSchedule> schedule = processor.initialize(context);
            if (schedule.isPresent()) {
                return Optionals.of(new ConcurrentLinkedQueue<>(schedule.get().getTasks()));
            } else {
                return Optionals.empty();
            }
        }

        void threadLocalInitialize(
                ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
            if (taskProcessor.get() != null) {
                LOG.warn(MessageFormat.format(
                        "task processor is already prepared: vertex={0}, thread={1}", //$NON-NLS-1$
                        vertexMirror.getName(),
                        Thread.currentThread().getName()));
                return;
            }
            taskProcessor.set(vertexProcessor.createTaskProcessor());
        }

        void threadLocalFinalize(
                ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
            try (TaskProcessor p = taskProcessor.get()) {
                if (p == null) {
                    LOG.warn(MessageFormat.format(
                            "task processor is not available: vertex={0}, thread={1}", //$NON-NLS-1$
                            vertexMirror.getName(),
                            Thread.currentThread().getName()));
                    return;
                }
                taskProcessor.remove();
            }
        }

        void run(ProcessorContext context, TaskMirror task) throws IOException, InterruptedException {
            TaskProcessor p = taskProcessor.get();
            Invariants.requireNonNull(p, () -> MessageFormat.format(
                    "task processor is not available: vertex={0}, thread={1}", //$NON-NLS-1$
                    vertexMirror.getName(),
                    Thread.currentThread().getName()));
            TaskInfo info = null;
            if (taskQueue.isPresent()) {
                info = taskQueue.get().poll();
                Invariants.requireNonNull(info, () -> MessageFormat.format(
                        "no available tasks: {0}", //$NON-NLS-1$
                        vertexMirror.getName()));
                if (LOG.isTraceEnabled()) {
                    LOG.trace(MessageFormat.format(
                            "custom task: {3} - {0}:{1} ({2})", //$NON-NLS-1$
                            vertexMirror,
                            vertexLabel,
                            task,
                            info));
                }
            }
            TaskProcessorContext c = decorator.bless(new TaskContext(context, vertexMirror, task, info));
            p.run(c);
        }

        int taskCount() {
            return numberOfTasks;
        }

        int maxConcurrency() {
            return vertexProcessor.getMaxConcurrency();
        }

        @Override
        public void close() throws IOException, InterruptedException {
            vertexProcessor.close();
            if (LOG.isWarnEnabled()) {
                int rest = taskQueue.map(Collection::size).orElse(0);
                if (rest > 0) {
                    LOG.warn(MessageFormat.format(
                            "vertex has incompleted tasks: {0} ({1}), rest={2}",
                            vertexMirror.getName(),
                            vertexLabel,
                            rest));
                }
            }
            if (LOG.isInfoEnabled()) {
                LOG.info(MessageFormat.format(
                        "finish vertex: {0} ({1})",
                        vertexMirror.getName(),
                        vertexLabel));
            }
        }

        @Override
        public String toString() {
            return vertexLabel;
        }
    }

    private abstract static class AbstractContext implements ForwardProcessorContext, EdgeIoProcessorContext {

        private final ProcessorContext root;

        final VertexMirror vertex;

        final TaskMirror task;

        AbstractContext(ProcessorContext root, VertexMirror vertex, TaskMirror task) {
            this.root = root;
            this.vertex = vertex;
            this.task = task;
        }

        @Override
        public ProcessorContext getForward() {
            return root;
        }

        @Override
        public EdgeReader getInput(String name) throws IOException, InterruptedException {
            PortMirror port = vertex.getInput(name);
            if (port.hasKey()) {
                // key value
                assert port.hasValue();
                ClassLoader loader = getClassLoader();
                InputReaderMirror reader = task.input(port.getId());
                return new KeyValueReaderBridge(reader, port.newKeyValueSerDe(loader));
            } else if (port.hasValue()) {
                // value only
                ClassLoader loader = getClassLoader();
                InputReaderMirror reader = task.input(port.getId());
                return new ValueReaderBridge(reader, port.newValueSerDe(loader));
            } else {
                // void
                throw new IllegalStateException(MessageFormat.format(
                        "input \"{0}\" ({1}) is void", //$NON-NLS-1$
                        vertex.getName(),
                        port.getName()));
            }
        }

        @Override
        public EdgeWriter getOutput(String name) throws IOException, InterruptedException {
            PortMirror port = vertex.getOutput(name);
            if (port.hasKey()) {
                // key value
                assert port.hasValue();
                ClassLoader loader = getClassLoader();
                OutputWriterMirror writer = task.output(port.getId());
                return new KeyValueWriterBridge(writer, port.newKeyValueSerDe(loader));
            } else if (port.hasValue()) {
                // value only
                OutputWriterMirror writer = task.output(port.getId());
                ClassLoader loader = getClassLoader();
                return new ValueWriterBridge(writer, port.newValueSerDe(loader));
            } else {
                // void
                throw new IllegalStateException(MessageFormat.format(
                        "output \"{0}\" ({1}) is void", //$NON-NLS-1$
                        vertex.getName(),
                        port.getName()));
            }
        }
    }

    private static class VertexContext
            extends AbstractContext
            implements VertexProcessorContext {

        VertexContext(ProcessorContext root, VertexMirror vertex, TaskMirror task) {
            super(root, vertex, task);
        }

        @Override
        public String getVertexId() {
            return vertex.getName();
        }
    }

    private static class TaskContext
            extends AbstractContext
            implements TaskProcessorContext {

        private final TaskInfo info;

        private final String id;

        TaskContext(ProcessorContext root, VertexMirror vertex, TaskMirror task, TaskInfo info) {
            super(root, vertex, task);
            this.info = info;
            this.id = String.format("%s-%s-%s", //$NON-NLS-1$
                    vertex.getName(),
                    task.logicalTaskId(),
                    task.phisicalTaskId());
        }

        @Override
        public String getVertexId() {
            return vertex.getName();
        }

        @Override
        public String getTaskId() {
            return id;
        }

        @Override
        public Optional<TaskInfo> getTaskInfo() {
            return Optionals.of(info);
        }
    }
}
