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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

import com.asakusafw.dag.api.common.ObjectCursor;
import com.asakusafw.dag.api.processor.GroupReader;
import com.asakusafw.dag.api.processor.ObjectReader;
import com.asakusafw.dag.api.processor.ObjectWriter;
import com.asakusafw.dag.api.processor.TaskProcessor;
import com.asakusafw.dag.api.processor.TaskSchedule;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.api.processor.VertexProcessorContext;
import com.asakusafw.dag.api.processor.basic.BasicProcessorContext;
import com.asakusafw.dag.api.processor.basic.BasicTaskInfo;
import com.asakusafw.dag.api.processor.basic.BasicTaskSchedule;
import com.asakusafw.dag.api.processor.basic.CoGroupReader;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Lang;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.lang.utils.common.Suppliers;
import com.asakusafw.lang.utils.common.Tuple;
import com.asakusafw.m3bp.descriptor.Descriptors;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.m3bp.mirror.basic.BasicVertexMirror;
import com.sun.jna.Memory;

/**
 * Test for {@link VertexExecutor}.
 */
public class VertexExecutorTest {

    /**
     * cleanup tests.
     */
    @Rule
    public final TestRule CLEANER = new ExternalResource() {
        @Override
        protected void after() {
            threads.shutdownNow();
        }
    };

    final BasicProcessorContext root = new BasicProcessorContext(getClass().getClassLoader());

    final ExecutorService threads = Executors.newCachedThreadPool();

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        VertexMirror vertex = new BasicVertexMirror("v", Bridge.wrap(new VertexProcessor() {
            @Override
            public Optional<? extends TaskSchedule> initialize(VertexProcessorContext c) {
                return Optionals.of(new BasicTaskSchedule(new BasicTaskInfo()));
            }
            @Override
            public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
                return c -> results.add("OK");
            }
        }));
        IO io = new IO();
        VertexExecutor exec = new VertexExecutor(root, vertex, io, threads, 1);
        exec.run();
        assertThat(results, contains("OK"));
    }

    /**
     * w/ input.
     * @throws Exception if failed
     */
    @Test
    public void input() throws Exception {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        VertexMirror vertex = new BasicVertexMirror("v", Bridge.wrap(new VertexProcessor() {
            @Override
            public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
                return context -> {
                    try (ObjectReader reader = (ObjectReader) context.getInput("port")) {
                        while (reader.nextObject()) {
                            results.add((Integer) reader.getObject());
                        }
                    }
                };
            }
        }));
        IO io = new IO();
        PortMirror port = vertex.addInput("port", io.newOneToOne());
        io.input(port, new int[] { 100, 200, 300, });
        VertexExecutor exec = new VertexExecutor(root, vertex, io, threads, 1);
        exec.run();

        assertThat(results, containsInAnyOrder(100, 200, 300));
    }

    /**
     * w/ gather input.
     * @throws Exception if failed
     */
    @SuppressWarnings("unchecked")
    @Test
    public void gather() throws Exception {
        List<Tuple<List<Integer>, List<Integer>>> results = Collections.synchronizedList(new ArrayList<>());
        VertexMirror vertex = new BasicVertexMirror("v", Bridge.wrap(new VertexProcessor() {
            @Override
            public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
                return context -> {
                    try (GroupReader g0 = (GroupReader) context.getInput("p0");
                            GroupReader g1 = (GroupReader) context.getInput("p1");
                            CoGroupReader reader = new CoGroupReader(g0, g1)) {
                        while (reader.nextCoGroup()) {
                            results.add(new Tuple<>(dump(reader.getGroup(0)), dump(reader.getGroup(1))));
                        }
                    }
                };
            }
        }));
        IO io = new IO();
        PortMirror p0 = vertex.addInput("p0", io.newScatterGather());
        PortMirror p1 = vertex.addInput("p1", io.newScatterGather());
        io.inputPairs(p0, 1, 100, 2, 200);
        io.inputPairs(p1, 2, 201, 3, 301);
        VertexExecutor exec = new VertexExecutor(root, vertex, io, threads, 1);
        exec.run();

        assertThat(results, containsInAnyOrder(
                new Tuple<>(Arrays.asList(100), Arrays.asList()),
                new Tuple<>(Arrays.asList(200), Arrays.asList(201)),
                new Tuple<>(Arrays.asList(), Arrays.asList(301))));
    }

    /**
     * w/ broadcast.
     * @throws Exception if failed
     */
    @Test
    public void broadcast() throws Exception {
        List<Integer> main = Collections.synchronizedList(new ArrayList<>());
        List<Integer> broadcast = Collections.synchronizedList(new ArrayList<>());
        VertexMirror vertex = new BasicVertexMirror("v", Bridge.wrap(new VertexProcessor() {
            @Override
            public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) throws IOException, InterruptedException {
                try (ObjectReader reader = (ObjectReader) context.getInput("broadcast")) {
                    broadcast.addAll(dump(reader));
                }
                return Optionals.of(null);
            }
            @Override
            public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
                return context -> {
                    try (ObjectReader reader = (ObjectReader) context.getInput("main")) {
                        main.addAll(dump(reader));
                    }
                };
            }
        }));
        IO io = new IO();
        PortMirror mainPort = vertex.addInput("main", io.newOneToOne());
        io.input(mainPort, 100).input(mainPort, 200).input(mainPort, 300);

        PortMirror broadcastPort = vertex.addInput("broadcast", io.newBroadcast());
        io.input(broadcastPort, 10, 20, 30);

        VertexExecutor exec = new VertexExecutor(root, vertex, io, threads, 1);
        exec.run();

        assertThat(main, containsInAnyOrder(100, 200, 300));
        assertThat(broadcast, containsInAnyOrder(10, 20, 30));
    }

    /**
     * w/ output.
     * @throws Exception if failed
     */
    @Test
    public void output() throws Exception {
        VertexMirror vertex = new BasicVertexMirror("v", Bridge.wrap(new VertexProcessor() {
            @Override
            public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
                return Optionals.of(new BasicTaskSchedule(new BasicTaskInfo()));
            }
            @Override
            public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
                return context -> {
                    try (ObjectWriter writer = (ObjectWriter) context.getOutput("port")) {
                        writer.putObject(100);
                    }
                };
            }
        }));
        IO io = new IO();
        PortMirror output = vertex.addOutput("port", io.newOneToOne());
        VertexExecutor exec = new VertexExecutor(root, vertex, io, threads, 1);
        exec.run();

        List<Integer> values = io.getOutputValues(output);
        assertThat(values, containsInAnyOrder(100));
    }

    /**
     * w/ scatter output.
     * @throws Exception if failed
     */
    @SuppressWarnings("unchecked")
    @Test
    public void scatter() throws Exception {
        VertexMirror vertex = new BasicVertexMirror("v", Bridge.wrap(new VertexProcessor() {
            @Override
            public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
                return Optionals.of(new BasicTaskSchedule(new BasicTaskInfo()));
            }
            @Override
            public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
                return context -> {
                    try (ObjectWriter writer = (ObjectWriter) context.getOutput("p0")) {
                        writer.putObject(100);
                    }
                    try (ObjectWriter writer = (ObjectWriter) context.getOutput("p1")) {
                        writer.putObject(201);
                        writer.putObject(301);
                    }
                };
            }
        }));
        IO io = new IO();
        PortMirror p0 = vertex.addOutput("p0", io.newScatterGather());
        PortMirror p1 = vertex.addOutput("p1", io.newScatterGather());
        VertexExecutor exec = new VertexExecutor(root, vertex, io, threads, 1);
        exec.run();

        assertThat(io.getOutputPairs(p0), containsInAnyOrder(new Tuple<>(100, 100)));
        assertThat(io.getOutputPairs(p1), containsInAnyOrder(new Tuple<>(201, 201), new Tuple<>(301, 301)));
    }

    /**
     * A {@link VertexProcessor} bridge.
     */
    public static class Bridge implements VertexProcessor {

        static VertexProcessor delegate;

        static M3bpVertexDescriptor wrap(VertexProcessor processor) {
            delegate = processor;
            return Descriptors.newVertex(Bridge.class);
        }

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) throws IOException, InterruptedException {
            return delegate.initialize(context);
        }

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return delegate.createTaskProcessor();
        }

        @Override
        public void close() throws IOException, InterruptedException {
            delegate.close();
            delegate = null;
        }
    }

    static List<Integer> dump(ObjectCursor cursor) {
        return Lang.let(new ArrayList<>(), results -> Lang.safe(() -> {
            while (cursor.nextObject()) {
                results.add((Integer) cursor.getObject());
            }
        }));
    }

    private static class IO implements IoMap {

        private final Map<PortMirror, List<InputBufferCursor>> inputs = new HashMap<>();

        private final Map<PortMirror, List<OutputBufferFragment>> outputs = new HashMap<>();

        /**
         * Creates a new instance.
         */
        public IO() {
            return;
        }

        IO input(PortMirror input, int... values) {
            int entries = values.length;
            Memory contents = new Memory(entries * Integer.BYTES);
            Memory offsets = new Memory((entries + 1) * Long.BYTES);
            offsets.setLong(0, 0);
            for (int i = 0; i < entries; i++) {
                contents.setInt(i * Integer.BYTES, values[i]);
                offsets.setLong((i + 1) * Long.BYTES, (i + 1) * Integer.BYTES);
            }
            InputBufferFragment fragment = new InputBufferFragment(contents, offsets, entries);
            return input(input, new InputBufferCursor(Suppliers.supplier(fragment)));
        }

        IO inputPairs(PortMirror input, int... keyValuePairs) {
            Arguments.require(keyValuePairs.length % 2 == 0);
            int entries = keyValuePairs.length / 2;
            Memory kContents = new Memory(entries * Integer.BYTES);
            Memory vContents = new Memory(entries * Integer.BYTES);
            Memory kOffsets = new Memory((entries + 1) * Long.BYTES);
            Memory vOffsets = new Memory((entries + 1) * Long.BYTES);
            kOffsets.setLong(0, 0);
            vOffsets.setLong(0, 0);
            for (int i = 0; i < entries; i++) {
                int k = keyValuePairs[i * 2 + 0];
                int v = keyValuePairs[i * 2 + 1];
                kContents.setInt(i * Integer.BYTES, k);
                vContents.setInt(i * Integer.BYTES, v);
                kOffsets.setLong((i + 1) * Long.BYTES, (i + 1) * Integer.BYTES);
                vOffsets.setLong((i + 1) * Long.BYTES, (i + 1) * Integer.BYTES);
            }
            InputBufferFragment kFragments = new InputBufferFragment(kContents, kOffsets, entries);
            InputBufferFragment vFragments = new InputBufferFragment(vContents, vOffsets, entries);
            return input(input, new InputBufferCursor(Suppliers.supplier(kFragments), Suppliers.supplier(vFragments)));
        }

        IO input(PortMirror input, InputBufferCursor cursor) {
            inputs.computeIfAbsent(input, p -> new ArrayList<>()).add(cursor);
            return this;
        }

        @Override
        public Supplier<OutputBufferFragment> getOutputSource(PortMirror port) {
            return () -> new OutputBufferFragment(4 * 1024, 1024, port.hasKey());
        }

        @Override
        public Consumer<OutputBufferFragment> getOutputSink(PortMirror port) {
            return outputs.computeIfAbsent(port, p -> new ArrayList<>())::add;
        }

        @Override
        public List<InputBufferCursor> getInputSource(PortMirror port) {
            return Optionals.remove(inputs, port).orElseThrow(AssertionError::new);
        }

        M3bpEdgeDescriptor newOneToOne() {
            return Descriptors.newOneToOneEdge(IntSerDe.class);
        }

        M3bpEdgeDescriptor newScatterGather() {
            return Descriptors.newScatterGatherEdge(IntSerDe.class, null, null);
        }

        M3bpEdgeDescriptor newBroadcast() {
            return Descriptors.newBroadcastEdge(IntSerDe.class);
        }

        List<Integer> getOutputValues(PortMirror port) {
            List<OutputBufferFragment> fragments = Optionals.remove(outputs, port).orElseThrow(AssertionError::new);
            return Lang.let(new ArrayList<>(), results -> {
                fragments.stream().forEach(f -> f.forEachEntries(b -> {
                    assertThat(b.remaining(), is(Integer.BYTES));
                    results.add(b.getInt());
                }));
            });
        }

        List<Tuple<Integer, Integer>> getOutputPairs(PortMirror port) {
            List<OutputBufferFragment> fragments = Optionals.remove(outputs, port).orElseThrow(AssertionError::new);
            return Lang.let(new ArrayList<>(), results -> {
                fragments.stream().forEach(f -> f.forEachEntries((k, v) -> {
                    assertThat(k.remaining(), is(Integer.BYTES));
                    assertThat(v.remaining(), is(Integer.BYTES));
                    results.add(new Tuple<>(k.getInt(), v.getInt()));
                }));
            });
        }
    }
}
