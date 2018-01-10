/**
 * Copyright 2011-2018 Asakusa Framework Team.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Invariants;
import com.asakusafw.lang.utils.common.Lang;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.lang.utils.common.RunnableWithException;
import com.asakusafw.m3bp.mirror.ConfigurationMirror;
import com.asakusafw.m3bp.mirror.FlowGraphMirror;
import com.asakusafw.m3bp.mirror.Movement;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.utils.graph.Graph;
import com.asakusafw.utils.graph.Graphs;

/**
 * Executes {@link FlowGraphMirror}.
 */
public class FlowGraphExecutor implements RunnableWithException<Exception> {

    static final Logger LOG = LoggerFactory.getLogger(FlowGraphExecutor.class);

    private final ProcessorContext context;

    private final FlowGraphMirror graph;

    final ConfigurationMirror configuration;

    final Function<String, ? extends BufferComparator> comparators;

    /**
     * Creates a new instance.
     * @param context the root context
     * @param graph the target flow graph
     * @param configuration the current configuration
     * @param comparators the comparators
     */
    public FlowGraphExecutor(
            ProcessorContext context,
            FlowGraphMirror graph,
            ConfigurationMirror configuration,
            Function<String, ? extends BufferComparator> comparators) {
        Arguments.requireNonNull(context);
        Arguments.requireNonNull(graph);
        Arguments.requireNonNull(configuration);
        this.context = context;
        this.graph = graph;
        this.configuration = configuration;
        this.comparators = comparators;
    }

    @Override
    public void run() throws IOException, InterruptedException {
        Graph<VertexMirror> dependencies = Lang.let(Graphs.newInstance(), g -> graph.getVertices().stream()
                .peek(g::addNode)
                .forEach(v -> v.getInputs().stream()
                        .forEach(p -> p.getOpposites().stream()
                                .map(o -> o.getOwner())
                                .forEach(o -> g.addEdge(v, o)))));
        try (ThreadPool threads = new ThreadPool();
                IO io = new IO()) {
            for (VertexMirror vertex : Graphs.sortPostOrder(dependencies)) {
                VertexExecutor executor = new VertexExecutor(
                        context, vertex, io,
                        threads.executor, configuration.getMaxConcurrency());
                executor.run();
                io.resolve(vertex);
            }
        }
    }

    private class ThreadPool implements AutoCloseable {

        final ExecutorService executor;

        ThreadPool() {
            AtomicInteger counter = new AtomicInteger();
            this.executor = Executors.newFixedThreadPool(
                    configuration.getMaxConcurrency(),
                    r -> Lang.let(new Thread(r), t -> {
                        t.setName(String.format("work-%d", counter.incrementAndGet())); //$NON-NLS-1$
                        t.setDaemon(true);
                    }));
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }
    }

    private class IO implements IoMap, AutoCloseable {

        final Map<PortMirror, EdgeProcessor> inputs = new HashMap<>();

        final Map<PortMirror, List<OutputBufferFragment>> outputs = new HashMap<>();

        private final Set<PortMirror> finishedInputs = new HashSet<>();

        private final Set<PortMirror> finishedOutputs = new HashSet<>();

        IO() {
            return;
        }

        @Override
        public Supplier<OutputBufferFragment> getOutputSource(PortMirror port) {
            return () -> new OutputBufferFragment(
                    configuration.getOutputBufferSize(),
                    configuration.getOutputRecordsPerBuffer(),
                    port.hasKey());
        }

        @Override
        public Consumer<OutputBufferFragment> getOutputSink(PortMirror port) {
            List<OutputBufferFragment> buffers;
            synchronized (outputs) {
                Invariants.require(finishedOutputs.contains(port) == false);
                buffers = outputs.computeIfAbsent(port, p -> Collections.synchronizedList(new ArrayList<>()));
            }
            return buffers::add;
        }

        @Override
        public List<InputBufferCursor> getInputSource(PortMirror port) {
            EdgeProcessor edge;
            synchronized (inputs) {
                Invariants.require(finishedInputs.contains(port) == false);
                Invariants.require(inputs.containsKey(port));
                edge = inputs.get(port);
            }
            return edge.process();
        }

        void resolve(VertexMirror vertex) {
            synchronized (inputs) {
                vertex.getInputs().stream()
                    .filter(p -> p.getMovement() != Movement.NOTHING)
                    .forEach(p -> {
                        Invariants.require(finishedInputs.contains(p) == false);
                        finishedInputs.add(p);
                        inputs.remove(p);
                    });
            }
            Map<PortMirror, List<OutputBufferFragment>> scoped = Lang.let(new HashMap<>(), map -> {
                synchronized (outputs) {
                    vertex.getOutputs().stream()
                        .filter(p -> p.getMovement() != Movement.NOTHING)
                        .forEach(p -> {
                            Invariants.require(finishedOutputs.contains(p) == false);
                            finishedOutputs.add(p);
                            map.put(p, Optionals.remove(outputs, p).orElse(Collections.emptyList()));
                        });
                }
            });
            scoped.forEach(this::resolve);
        }

        private void resolve(PortMirror output, List<OutputBufferFragment> results) {
            synchronized (inputs) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("resolving edge: {}.{} ({})", //$NON-NLS-1$
                            output.getOwner().getName(), output.getName(), output.getMovement());
                }
                for (PortMirror input : output.getOpposites()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("  -> {}.{}", //$NON-NLS-1$
                                input.getOwner().getName(), input.getName());
                    }
                    Invariants.require(finishedInputs.contains(input) == false);
                    EdgeProcessor processor = inputs.computeIfAbsent(input, p -> {
                        switch (output.getMovement()) {
                        case ONE_TO_ONE:
                            return new MoveEdgeProcessor();
                        case BROADCAST:
                            return new BroadcastEdgeProcessor();
                        case SCATTER_GATHER:
                            return new ScatterGatherProcessor(
                                    configuration.getPartitionCount(),
                                    resolveComparator(output.getValueComparatorName()));
                        default:
                            throw new AssertionError(output);
                        }
                    });
                    processor.add(results);
                }
            }
        }

        private BufferComparator resolveComparator(String functionName) {
            if (functionName == null) {
                return null;
            }
            Invariants.requireNonNull(comparators);
            return comparators.apply(functionName);
        }

        @Override
        public void close() {
            return;
        }
    }
}
