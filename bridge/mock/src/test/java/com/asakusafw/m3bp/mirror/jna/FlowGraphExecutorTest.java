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
package com.asakusafw.m3bp.mirror.jna;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.Test;

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
import com.asakusafw.lang.utils.common.Lang;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.m3bp.descriptor.Descriptors;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.m3bp.mirror.basic.BasicConfigurationMirror;
import com.asakusafw.m3bp.mirror.basic.BasicFlowGraphMirror;

/**
 * Test for {@link FlowGraphExecutor}.
 */
public class FlowGraphExecutorTest {

    final BasicProcessorContext root = new BasicProcessorContext(getClass().getClassLoader());

    final BasicConfigurationMirror conf = new BasicConfigurationMirror();

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        Orphan.INPUT.clear();
        Orphan.OUTPUT.clear();

        BasicFlowGraphMirror graph = new BasicFlowGraphMirror();
        graph.addVertex("orphan", Descriptors.newVertex(Orphan.class));
        FlowGraphExecutor executor = new FlowGraphExecutor(root, graph, conf, null);

        Collections.addAll(Orphan.INPUT, "A", "B", "C");
        executor.run();
        assertThat(Orphan.OUTPUT, containsInAnyOrder("A", "B", "C"));

        Orphan.INPUT.clear();
        Orphan.OUTPUT.clear();
    }

    /**
     * one to one.
     * @throws Exception if failed
     */
    @Test
    public void one_to_one() throws Exception {
        Generator.INPUT.clear();
        Consumer.OUTPUT.clear();

        BasicFlowGraphMirror graph = new BasicFlowGraphMirror();

        VertexMirror generator = graph.addVertex("generator", Descriptors.newVertex(Generator.class));
        VertexMirror consumer = graph.addVertex("consumer", Descriptors.newVertex(Consumer.class));

        PortMirror out = generator.addOutput("out", Descriptors.newOneToOneEdge(StringSerDe.class));
        PortMirror in = consumer.addInput("in", Descriptors.newOneToOneEdge(StringSerDe.class));
        graph.addEdge(out, in);

        FlowGraphExecutor executor = new FlowGraphExecutor(root, graph, conf, null);

        Collections.addAll(Generator.INPUT, "A", "B", "C");
        executor.run();
        assertThat(Consumer.OUTPUT, containsInAnyOrder("A", "B", "C"));

        Generator.INPUT.clear();
        Consumer.OUTPUT.clear();
    }

    /**
     * broadcast.
     * @throws Exception if failed
     */
    @Test
    public void broadcast() throws Exception {
        Generator.INPUT.clear();
        BroadcastConsumer.OUTPUT.clear();

        BasicFlowGraphMirror graph = new BasicFlowGraphMirror();

        VertexMirror generator = graph.addVertex("generator", Descriptors.newVertex(Generator.class));
        VertexMirror consumer = graph.addVertex("consumer", Descriptors.newVertex(BroadcastConsumer.class));

        PortMirror out = generator.addOutput("out", Descriptors.newBroadcastEdge(StringSerDe.class));
        PortMirror in = consumer.addInput("in", Descriptors.newBroadcastEdge(StringSerDe.class));
        graph.addEdge(out, in);

        FlowGraphExecutor executor = new FlowGraphExecutor(root, graph, conf, null);

        Collections.addAll(Generator.INPUT, "A", "B", "C");
        executor.run();
        assertThat(BroadcastConsumer.OUTPUT, containsInAnyOrder("A", "B", "C"));

        Generator.INPUT.clear();
        BroadcastConsumer.OUTPUT.clear();
    }

    /**
     * word count.
     * @throws Exception if failed
     */
    @Test
    public void wordcount() throws Exception {
        Map<String, Integer> results = wordcount(new String[] {
                "Hello"
        });
        assertThat(results.keySet(), containsInAnyOrder("hello"));
        assertThat(results, hasEntry("hello", 1));
    }

    private Map<String, Integer> wordcount(String[] lines) throws IOException, InterruptedException {
        WcMap.TEXT.clear();
        WcReduce.RESULTS.clear();
        Collections.addAll(WcMap.TEXT, lines);

        BasicFlowGraphMirror graph = new BasicFlowGraphMirror();

        VertexMirror map = graph.addVertex("map", Descriptors.newVertex(WcMap.class));
        VertexMirror reduce = graph.addVertex("reduce", Descriptors.newVertex(WcReduce.class));

        PortMirror mapOut = map.addOutput("out", Descriptors.newScatterGatherEdge(
                StringSerDe.class, StringSerDe.class, StringSerDe.class.getName()));
        PortMirror reduceIn = reduce.addInput("in", Descriptors.newScatterGatherEdge(
                StringSerDe.class, StringSerDe.class, StringSerDe.class.getName()));

        graph.addEdge(mapOut, reduceIn);

        FlowGraphExecutor executor = new FlowGraphExecutor(root, graph, conf, new MockBufferComparatorProvider());
        executor.run();

        return Lang.let(new HashMap<>(), results -> {
            WcReduce.RESULTS.forEach((k, v) -> results.put(k, v.get()));
            WcMap.TEXT.clear();
            WcReduce.RESULTS.clear();
        });
    }

    /**
     * word count.
     * @throws Exception if failed
     */
    @Test
    public void wordcount2() throws Exception {
        Map<String, Integer> results = wordcount(
                new String[] { "Hello1" },
                new String[] { "Hello2" });
        assertThat(results.keySet(), containsInAnyOrder("hello1", "hello2"));
        assertThat(results, hasEntry("hello1", 1));
        assertThat(results, hasEntry("hello2", 1));
    }

    private Map<String, Integer> wordcount(String[] l1, String[] l2) throws IOException, InterruptedException {
        WcMap.TEXT.clear();
        WcMap2.TEXT2.clear();
        Collections.addAll(WcMap.TEXT, l1);
        Collections.addAll(WcMap2.TEXT2, l2);
        WcReduce.RESULTS.clear();

        BasicFlowGraphMirror graph = new BasicFlowGraphMirror();

        VertexMirror map1 = graph.addVertex("map1", Descriptors.newVertex(WcMap.class));
        VertexMirror map2 = graph.addVertex("map2", Descriptors.newVertex(WcMap2.class));
        VertexMirror reduce = graph.addVertex("reduce", Descriptors.newVertex(WcReduce.class));

        PortMirror map1Out = map1.addOutput("out", Descriptors.newScatterGatherEdge(
                StringSerDe.class, StringSerDe.class, StringSerDe.class.getName()));
        PortMirror map2Out = map2.addOutput("out", Descriptors.newScatterGatherEdge(
                StringSerDe.class, StringSerDe.class, StringSerDe.class.getName()));
        PortMirror reduceIn = reduce.addInput("in", Descriptors.newScatterGatherEdge(
                StringSerDe.class, StringSerDe.class, StringSerDe.class.getName()));

        graph.addEdge(map1Out, reduceIn);
        graph.addEdge(map2Out, reduceIn);

        FlowGraphExecutor executor = new FlowGraphExecutor(root, graph, conf, new MockBufferComparatorProvider());
        executor.run();

        return Lang.let(new HashMap<>(), results -> {
            WcReduce.RESULTS.forEach((k, v) -> results.put(k, v.get()));
            WcMap.TEXT.clear();
            WcMap2.TEXT2.clear();
            WcReduce.RESULTS.clear();
        });
    }

    /**
     * Standalone vertex.
     */
    public static class Orphan implements VertexProcessor {

        static final List<String> INPUT = new ArrayList<>();

        static final List<String> OUTPUT = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
            return Optionals.of(new BasicTaskSchedule(Lang.project(INPUT, BasicTaskInfo::new)));
        }

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return context -> {
                String string = context.getTaskInfo()
                        .map(BasicTaskInfo.class::cast)
                        .map(info -> (String) info.getValue())
                        .orElseThrow(AssertionError::new);
                OUTPUT.add(string);
            };
        }
    }

    /**
     * Generator vertex.
     */
    public static class Generator implements VertexProcessor {

        static final List<Object> INPUT = new ArrayList<>();

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
            return Optionals.of(new BasicTaskSchedule(Lang.project(INPUT, BasicTaskInfo::new)));
        }

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return context -> {
                try (ObjectWriter writer = (ObjectWriter) context.getOutput("out")) {
                    writer.putObject(context.getTaskInfo()
                        .map(BasicTaskInfo.class::cast)
                        .map(BasicTaskInfo::getValue)
                        .orElseThrow(AssertionError::new));
                }
            };
        }
    }

    /**
     * Consumer vertex.
     */
    public static class Consumer implements VertexProcessor {

        static final List<Object> OUTPUT = Collections.synchronizedList(new ArrayList<>());

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return context -> {
                try (ObjectReader reader = (ObjectReader) context.getInput("in")) {
                    while (reader.nextObject()) {
                        OUTPUT.add(reader.getObject());
                    }
                }
            };
        }
    }

    /**
     * Broadcast consumer vertex.
     */
    public static class BroadcastConsumer implements VertexProcessor {

        static final List<Object> OUTPUT = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Optional<? extends TaskSchedule> initialize(
                VertexProcessorContext context) throws IOException, InterruptedException {
            try (ObjectReader reader = (ObjectReader) context.getInput("in")) {
                while (reader.nextObject()) {
                    OUTPUT.add(reader.getObject());
                }
            }
            return Optional.of(new BasicTaskSchedule());
        }

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Word count mapper.
     */
    public static class WcMap implements VertexProcessor {

        static final List<String> TEXT = new ArrayList<>();

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
            return Optionals.of(new BasicTaskSchedule(Lang.project(TEXT, BasicTaskInfo::new)));
        }

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return context -> {
                String line = context.getTaskInfo()
                        .map(BasicTaskInfo.class::cast)
                        .map(info -> (String) info.getValue())
                        .orElseThrow(AssertionError::new);
                try (ObjectWriter writer = (ObjectWriter) context.getOutput("out")) {
                    Stream.of(line.split("\\s+"))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(s -> !s.isEmpty())
                        .forEach(s -> Lang.safe(() -> writer.putObject(s)));
                }
            };
        }
    }

    /**
     * Word count mapper.
     */
    public static class WcMap2 extends WcMap {

        static final List<String> TEXT2 = new ArrayList<>();

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
            return Optionals.of(new BasicTaskSchedule(Lang.project(TEXT2, BasicTaskInfo::new)));
        }
    }

    /**
     * Word count reducer.
     */
    public static class WcReduce implements VertexProcessor {

        static final Map<String, AtomicInteger> RESULTS = new HashMap<>();

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return context -> {
                try (GroupReader reader = (GroupReader) context.getInput("in")) {
                    while (reader.nextGroup()) {
                        while (reader.nextObject()) {
                            String s = (String) reader.getObject();
                            RESULTS.computeIfAbsent(s, it -> new AtomicInteger()).incrementAndGet();
                        }
                    }
                }
            };
        }
    }
}
