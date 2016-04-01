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
package com.asakusafw.m3bp.mirror.jna;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.common.KeyValueSerDe;
import com.asakusafw.dag.api.common.ValueSerDe;
import com.asakusafw.dag.api.processor.EdgeIoProcessorContext;
import com.asakusafw.dag.api.processor.EdgeReader;
import com.asakusafw.dag.api.processor.EdgeWriter;
import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.api.processor.TaskInfo;
import com.asakusafw.dag.api.processor.TaskProcessorContext;
import com.asakusafw.dag.api.processor.TaskSchedule;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.api.processor.VertexProcessorContext;
import com.asakusafw.dag.api.processor.basic.ForwardProcessorContext;
import com.asakusafw.dag.api.processor.extension.ProcessorContextDecorator;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.dag.utils.common.Lang;
import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.dag.utils.common.RunnableWithException;
import com.asakusafw.m3bp.bridge.KeyValueReaderBridge;
import com.asakusafw.m3bp.bridge.KeyValueWriterBridge;
import com.asakusafw.m3bp.bridge.ValueReaderBridge;
import com.asakusafw.m3bp.bridge.ValueWriterBridge;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.Movement;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * Executes vertices.
 */
class VertexExecutor implements RunnableWithException<Exception> {

    static final Logger LOG = LoggerFactory.getLogger(VertexExecutor.class);

    final ProcessorContext context;

    final VertexMirror vertex;

    final IoMap ios;

    private final ExecutorService executor;

    private final int maxConcurrency;

    private final ProcessorContextDecorator decorator;

    /**
     * Creates a new instance.
     * @param context the root context
     * @param vertex the target vertex
     * @param ios the I/O map
     * @param threads the task executor
     * @param maxConcurrency the task max concurrency
     */
    VertexExecutor(
            ProcessorContext context,
            VertexMirror vertex,
            IoMap ios,
            ExecutorService threads,
            int maxConcurrency) {
        Arguments.requireNonNull(context);
        Arguments.requireNonNull(vertex);
        Arguments.requireNonNull(ios);
        Arguments.requireNonNull(threads);
        Arguments.require(maxConcurrency >= 1);
        this.context = context;
        this.vertex = vertex;
        this.ios = ios;
        this.executor = threads;
        this.maxConcurrency = maxConcurrency;
        this.decorator = context.getResource(ProcessorContextDecorator.class)
                .orElse(ProcessorContextDecorator.NULL);
    }

    @Override
    public void run() throws IOException, InterruptedException {
        try (VertexProcessor processor = vertex.newProcessor(context.getClassLoader())) {
            if (LOG.isDebugEnabled()) {
                dump(vertex, processor);
            }
            LOG.debug("initializing vertex: {}", vertex.getName()); //$NON-NLS-1$
            BlockingQueue<TaskProcessorContext> tasks = doInitialize(processor);
            int concurrency = computeConcurrency(tasks.size());
            LOG.debug("submitting tasks: vertex={}, count={}, concurrency={}",
                    vertex.getName(), tasks.size(), concurrency);
            LinkedList<Future<?>> futures = Lang.let(new LinkedList<>(), it -> Lang.repeat(concurrency, () -> {
                TaskExecutor child = new TaskExecutor(processor, tasks);
                it.add(executor.submit(() -> {
                    // this block must be a callable to throw exceptions
                    child.run();
                    return null;
                }));
            }));
            while (futures.isEmpty() == false) {
                Future<?> first = futures.removeFirst();
                try {
                    first.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("waiting for task completion: {}", first, e); //$NON-NLS-1$
                    }
                    futures.addLast(first);
                } catch (ExecutionException e) {
                    Throwable t = e.getCause();
                    Lang.rethrow(t, Error.class);
                    Lang.rethrow(t, RuntimeException.class);
                    Lang.rethrow(t, IOException.class);
                    Lang.rethrow(t, InterruptedException.class);
                    throw new IOException(t);
                }
            }
            LOG.debug("finalizing vertex: {}", vertex.getName()); //$NON-NLS-1$
        }
    }

    private static void dump(VertexMirror vertex, VertexProcessor processor) {
        LOG.debug("initializing vertex: {} ({})", vertex.getName(), processor); //$NON-NLS-1$
        for (PortMirror port : vertex.getInputs()) {
            LOG.debug("  input: {}.{} ({})", vertex.getName(), port.getName(), port.getMovement()); //$NON-NLS-1$
            for (PortMirror opposite : port.getOpposites()) {
                LOG.debug("    <- {}.{}", opposite.getOwner().getName(), opposite.getName()); //$NON-NLS-1$
            }
        }
        for (PortMirror port : vertex.getOutputs()) {
            LOG.debug("  output: {}.{} ({})", vertex.getName(), port.getName(), port.getMovement()); //$NON-NLS-1$
            for (PortMirror opposite : port.getOpposites()) {
                LOG.debug("    -> {}.{}", opposite.getOwner().getName(), opposite.getName()); //$NON-NLS-1$
            }
        }
    }

    private BlockingQueue<TaskProcessorContext> doInitialize(
            VertexProcessor processor) throws IOException, InterruptedException {
        VertexContext local = new VertexContext(vertex);
        List<PortMirror> broadcastInputs = vertex.getInputs().stream()
                .filter(p -> p.getMovement() == Movement.BROADCAST)
                .collect(Collectors.<PortMirror>toList());
        Lang.forEach(broadcastInputs, p -> {
            List<InputBufferCursor> source = ios.getInputSource(p);
            Invariants.require(source.size() == 1);
            local.input(p, source.get(0));
        });
        Optional<? extends TaskSchedule> schedule = processor.initialize(decorator.bless(local));

        Map<PortMirror, List<InputBufferCursor>> inputs = vertex.getInputs().stream()
                .filter(p -> p.getMovement() == Movement.ONE_TO_ONE || p.getMovement() == Movement.SCATTER_GATHER)
                .collect(Collectors.<PortMirror, PortMirror, List<InputBufferCursor>>toMap(
                        Function.identity(), ios::getInputSource));
        if (inputs.isEmpty()) {
            AtomicInteger sequence = new AtomicInteger(0);
            return schedule.orElseThrow(IllegalStateException::new).getTasks().stream()
                    .map(t -> new TaskContext(t, vertex.getName(), sequence.incrementAndGet()))
                    .map(decorator::bless)
                    .collect(Collectors.toCollection(LinkedBlockingQueue::new));
        } else {
            schedule.ifPresent(s -> {
                throw new IllegalStateException();
            });
            List<Integer> sizes = inputs.values().stream()
                .map(Collection::size)
                .distinct()
                .collect(Collectors.toList());
            Invariants.require(sizes.size() == 1);
            int tasks = sizes.get(0);
            return Lang.let(new LinkedBlockingQueue<>(), q -> {
                Lang.repeat(tasks, id -> {
                    TaskContext tc = new TaskContext(vertex.getName(), id);
                    inputs.forEach((k, v) -> tc.input(k, v.get(id)));
                    q.add(decorator.bless(tc));
                });
            });
        }
    }

    private int computeConcurrency(int numberOfTasks) {
        if (numberOfTasks <= 0) {
            return 0;
        }
        int max = numberOfTasks;
        if (maxConcurrency >= 1) {
            max = Math.min(max, maxConcurrency);
        }
        if (vertex.getMaxConcurrency() >= 1) {
            max = Math.min(max, vertex.getMaxConcurrency());
        }
        return max;
    }

    private class Context implements EdgeIoProcessorContext, ForwardProcessorContext {

        private final Map<PortMirror, InputBufferCursor> inputs = new LinkedHashMap<>();

        Context() {
            return;
        }

        @Override
        public ProcessorContext getForward() {
            return VertexExecutor.this.context;
        }

        @Override
        public EdgeReader getInput(String name) throws IOException, InterruptedException {
            Arguments.requireNonNull(name);
            PortMirror port = vertex.getInput(name);
            if (port == null) {
                throw new IOException(name);
            }
            InputBufferCursor input = inputs.get(port);
            if (input == null) {
                throw new IOException(name);
            }
            if (port.hasKey()) {
                KeyValueSerDe serde = port.newKeyValueSerDe(getClassLoader());
                InputReaderMirror mirror = new BufferInputReaderMirror(input);
                return new KeyValueReaderBridge(mirror, serde);
            } else {
                ValueSerDe deser = port.newValueSerDe(getClassLoader());
                InputReaderMirror mirror = new BufferInputReaderMirror(input);
                return new ValueReaderBridge(mirror, deser);
            }
        }

        @Override
        public EdgeWriter getOutput(String name) throws IOException, InterruptedException {
            Arguments.requireNonNull(name);
            PortMirror port = vertex.getOutput(name);
            if (port == null) {
                throw new IOException(name);
            }
            Supplier<OutputBufferFragment> source = VertexExecutor.this.ios.getOutputSource(port);
            Consumer<OutputBufferFragment> sink = VertexExecutor.this.ios.getOutputSink(port);
            if (port.hasKey()) {
                OutputWriterMirror mirror = new BufferOutputWriterMirror(source, sink);
                KeyValueSerDe serde = port.newKeyValueSerDe(getClassLoader());
                return new KeyValueWriterBridge(mirror, serde);
            } else {
                OutputWriterMirror mirror = new BufferOutputWriterMirror(source, sink);
                ValueSerDe serde = port.newValueSerDe(getClassLoader());
                return new ValueWriterBridge(mirror, serde);
            }
        }

        void input(PortMirror port, InputBufferCursor source) {
            inputs.put(port, source);
        }
    }

    private class VertexContext extends Context implements VertexProcessorContext {

        private final VertexMirror mirror;

        VertexContext(VertexMirror mirror) {
            this.mirror = mirror;
        }

        @Override
        public String getVertexId() {
            return mirror.getName();
        }
    }

    private class TaskContext extends Context implements TaskProcessorContext {

        private final String vertexId;

        private final TaskInfo info;

        private final int taskId;

        TaskContext(String vertexId, int taskId) {
            this(null, vertexId, taskId);
        }

        TaskContext(TaskInfo info, String vertexId, int taskId) {
            this.info = info;
            this.vertexId = vertexId;
            this.taskId = taskId;
        }

        @Override
        public String getVertexId() {
            return vertexId;
        }

        @Override
        public String getTaskId() {
            return String.format("%s-%d", //$NON-NLS-1$
                    VertexExecutor.this.vertex.getName(),
                    taskId);
        }

        @Override
        public Optional<TaskInfo> getTaskInfo() {
            return Optionals.of(info);
        }
    }
}
