/**
 * Copyright 2011-2021 Asakusa Framework Team.
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

import static com.asakusafw.m3bp.descriptor.Descriptors.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.processor.GroupReader;
import com.asakusafw.dag.api.processor.ObjectReader;
import com.asakusafw.dag.api.processor.ObjectWriter;
import com.asakusafw.dag.api.processor.TaskInfo;
import com.asakusafw.dag.api.processor.TaskProcessor;
import com.asakusafw.dag.api.processor.TaskProcessorContext;
import com.asakusafw.dag.api.processor.TaskSchedule;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.api.processor.VertexProcessorContext;
import com.asakusafw.dag.api.processor.basic.BasicProcessorContext;
import com.asakusafw.dag.api.processor.basic.BasicTaskSchedule;
import com.asakusafw.lang.utils.common.Lang;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.mirror.ConfigurationMirror;
import com.asakusafw.m3bp.mirror.ConfigurationMirror.BufferAccessMode;
import com.asakusafw.m3bp.mirror.EngineMirror;
import com.asakusafw.m3bp.mirror.FlowGraphMirror;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.m3bp.mirror.unsafe.UnsafeUtil;

/**
 * Test for {@link EngineMirrorImpl}.
 */
@RunWith(Parameterized.class)
public class EngineMirrorImplTest {

    static final Logger LOG = LoggerFactory.getLogger(EngineMirrorImplTest.class);

    private static final File TEST_LIB = new File(
            "target/native/test",
            System.mapLibraryName("m3bpjni-test")).getAbsoluteFile();

    /**
     * Checks native library is enabled.
     */
    @ClassRule
    public static final NativeEnabled NATIVE = new NativeEnabled();

    /**
     * Cleanup mock vertex processors.
     */
    @Rule
    public final ExternalResource cleanup = new ExternalResource() {
        @Override
        protected void after() {
            CallbackProcessor.TASKS.clear();
            ValueProducerProcessor.VALUES.clear();
            ObjectConsumerProcessor.RECEIVED.clear();
            GroupConsumerProcessor.RECEIVED.clear();
        }
    };

    /**
     * Returns the parameters.
     * @return the parameters
     */
    @Parameters(name = "{0}")
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "generic", (Consumer<ConfigurationMirror>) c -> {
                    return;
                }},
                { "unsafe", (Consumer<ConfigurationMirror>) c -> {
                    Assume.assumeTrue(UnsafeUtil.isAvailable());
                    c.withBufferAccessMode(BufferAccessMode.UNSAFE);
                }},
        });
    }

    private final Consumer<ConfigurationMirror> configurator;

    /**
     * Creates a new instance.
     * @param label the parameter label
     * @param configurator the configurator
     */
    public EngineMirrorImplTest(String label, Consumer<ConfigurationMirror> configurator) {
        this.configurator = configurator;
    }

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        try (EngineMirror mirror = new EngineMirrorImpl(null)) {
            configurator.accept(mirror.getConfiguration());
            FlowGraphMirror graph = mirror.getGraph();
            graph.addVertex("simple", newVertex(DummyProcessor.class));
            mirror.run(new BasicProcessorContext(getClass().getClassLoader()));
        }
    }

    /**
     * Dummy {@link VertexProcessor}.
     */
    public static class DummyProcessor implements VertexProcessor {

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * executes single task.
     * @throws Exception if failed
     */
    @Test
    public void task_single() throws Exception {
        try (EngineMirror mirror = new EngineMirrorImpl(null)) {
            AtomicBoolean done = new AtomicBoolean();
            CallbackProcessor.TASKS.add(c -> {
                assertThat(done.compareAndSet(false, true), is(true));
            });

            configurator.accept(mirror.getConfiguration());
            FlowGraphMirror graph = mirror.getGraph();
            graph.addVertex("simple", newVertex(CallbackProcessor.class));

            assertThat(done.get(), is(false));
            mirror.run(new BasicProcessorContext(getClass().getClassLoader()));
            assertThat(done.get(), is(true));
        }
    }

    /**
     * executes erroneous task.
     * @throws Exception required
     */
    @Test(expected = IOException.class)
    public void task_erroneous() throws Exception {
        try (EngineMirror mirror = new EngineMirrorImpl(null)) {
            CallbackProcessor.TASKS.add(c -> {
                throw new IOException("OK");
            });
            configurator.accept(mirror.getConfiguration());
            FlowGraphMirror graph = mirror.getGraph();
            graph.addVertex("simple", newVertex(CallbackProcessor.class));
            mirror.run(new BasicProcessorContext(getClass().getClassLoader()));
        }
    }

    /**
     * executes multiple tasks.
     * @throws Exception if failed
     */
    @Test
    public void task_multiple() throws Exception {
        try (EngineMirror mirror = new EngineMirrorImpl(null)) {
            AtomicBoolean done0 = new AtomicBoolean();
            CallbackProcessor.TASKS.add(c -> {
                assertThat(done0.compareAndSet(false, true), is(true));
            });
            AtomicBoolean done1 = new AtomicBoolean();
            CallbackProcessor.TASKS.add(c -> {
                assertThat(done1.compareAndSet(false, true), is(true));
            });
            AtomicBoolean done2 = new AtomicBoolean();
            CallbackProcessor.TASKS.add(c -> {
                assertThat(done2.compareAndSet(false, true), is(true));
            });

            configurator.accept(mirror.getConfiguration());
            FlowGraphMirror graph = mirror.getGraph();
            graph.addVertex("simple", newVertex(CallbackProcessor.class));

            assertThat(done0.get(), is(false));
            assertThat(done1.get(), is(false));
            assertThat(done2.get(), is(false));
            mirror.run(new BasicProcessorContext(getClass().getClassLoader()));
            assertThat(done0.get(), is(true));
            assertThat(done1.get(), is(true));
            assertThat(done2.get(), is(true));
        }
    }

    /**
     * callback processor.
     */
    public static class CallbackProcessor implements VertexProcessor {

        static final List<IoAction<TaskProcessorContext>> TASKS = new ArrayList<>();

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
            assertThat(TASKS, is(not(empty())));
            return Optional.of(new BasicTaskSchedule(Lang.project(TASKS, c -> (Info) x -> c.perform(x))));
        }

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return c -> c.getTaskInfo()
                    .map(i -> (Info) i)
                    .orElseThrow(AssertionError::new)
                    .run(c);
        }

        @FunctionalInterface
        private interface Info extends TaskInfo {
            void run(TaskProcessorContext context) throws IOException, InterruptedException;
        }
    }

    /**
     * executes tasks w/ I/O.
     * @throws Exception if failed
     */
    @Test
    public void task_io() throws Exception {
        try (EngineMirror mirror = new EngineMirrorImpl(null)) {
            M3bpEdgeDescriptor edge = newOneToOneEdge(IntSerDe.class);
            configurator.accept(mirror.getConfiguration());
            FlowGraphMirror graph = mirror.getGraph();

            VertexMirror producer = graph.addVertex("producer", newVertex(ValueProducerProcessor.class));
            PortMirror up = producer.addOutput("out", edge);

            VertexMirror consumer = graph.addVertex("consumer", newVertex(ObjectConsumerProcessor.class));
            PortMirror down = consumer.addInput("in", edge);

            graph.addEdge(up, down);

            ValueProducerProcessor.VALUES.add(new Object[] { });
            ValueProducerProcessor.VALUES.add(new Object[] { 1 });
            ValueProducerProcessor.VALUES.add(new Object[] { 2, 3 });
            ValueProducerProcessor.VALUES.add(new Object[] { 4, 5, 6 });
            assertThat(ObjectConsumerProcessor.RECEIVED, is(empty()));
            mirror.run(new BasicProcessorContext(getClass().getClassLoader()));
            assertThat(ObjectConsumerProcessor.RECEIVED.stream().collect(Collectors.toSet()),
                    is(Stream.of(1, 2, 3, 4, 5, 6).collect(Collectors.toSet())));
        }
    }

    /**
     * executes tasks w/ I/O.
     * @throws Exception if failed
     */
    @Test
    public void task_scatter_gather() throws Exception {
        try (EngineMirror mirror = new EngineMirrorImpl(null)) {
            configurator.accept(mirror.getConfiguration());
            FlowGraphMirror graph = mirror.getGraph();

            M3bpEdgeDescriptor edge = newScatterGatherEdge(IntModSerDe.class, null, null);
            VertexMirror producer = graph.addVertex("producer", newVertex(ValueProducerProcessor.class));
            PortMirror up = producer.addOutput("out", edge);
            VertexMirror consumer = graph.addVertex("consumer", newVertex(GroupConsumerProcessor.class));
            PortMirror down = consumer.addInput("in", edge);

            graph.addEdge(up, down);

            ValueProducerProcessor.VALUES.add(new Object[] { });
            ValueProducerProcessor.VALUES.add(new Object[] { 10, });
            ValueProducerProcessor.VALUES.add(new Object[] { 20, 21, });
            ValueProducerProcessor.VALUES.add(new Object[] { 30, 31, 32, });
            assertThat(GroupConsumerProcessor.RECEIVED, is(empty()));
            mirror.run(new BasicProcessorContext(getClass().getClassLoader()));

            System.out.println(GroupConsumerProcessor.RECEIVED);
            assertThat(GroupConsumerProcessor.RECEIVED, containsInAnyOrder(Arrays.asList(
                    containsInAnyOrder(10, 20, 30),
                    containsInAnyOrder(21, 31),
                    containsInAnyOrder(32))));
        }
    }

    /**
     * executes tasks w/ I/O.
     * @throws Exception if failed
     */
    @Test
    public void task_scatter_gather_sorted() throws Exception {
        Assume.assumeTrue(TEST_LIB.exists());
        try (EngineMirror mirror = new EngineMirrorImpl(TEST_LIB)) {
            configurator.accept(mirror.getConfiguration());
            FlowGraphMirror graph = mirror.getGraph();

            M3bpEdgeDescriptor edge = newScatterGatherEdge(
                    IntModSerDe.class, IntModSerDe.class, "lt_int32");
            VertexMirror producer = graph.addVertex("producer", newVertex(ValueProducerProcessor.class));
            PortMirror up = producer.addOutput("out", edge);
            VertexMirror consumer = graph.addVertex("consumer", newVertex(GroupConsumerProcessor.class));
            PortMirror down = consumer.addInput("in", edge);

            graph.addEdge(up, down);

            ValueProducerProcessor.VALUES.add(new Object[] { });
            ValueProducerProcessor.VALUES.add(new Object[] { 10, });
            ValueProducerProcessor.VALUES.add(new Object[] { 20, 21, });
            ValueProducerProcessor.VALUES.add(new Object[] { 30, 31, 32, });
            assertThat(GroupConsumerProcessor.RECEIVED, is(empty()));
            mirror.run(new BasicProcessorContext(getClass().getClassLoader()));

            System.out.println(GroupConsumerProcessor.RECEIVED);
            assertThat(GroupConsumerProcessor.RECEIVED, containsInAnyOrder(Arrays.asList(
                    contains(10, 20, 30),
                    contains(21, 31),
                    contains(32))));
        }
    }

    /**
     * value receiver.
     */
    public static class ValueProducerProcessor implements VertexProcessor {

        static final List<Object[]> VALUES = new ArrayList<>();

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
            assertThat(VALUES, is(not(empty())));
            return Optional.of(new BasicTaskSchedule(Lang.project(VALUES, Info::new)));
        }

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return c -> {
                Object[] values = c.getTaskInfo()
                        .map(v -> (Info) v)
                        .map(i -> i.values)
                        .orElseThrow(AssertionError::new);
                try (ObjectWriter w = (ObjectWriter) c.getOutput("out")) {
                    for (Object o : values) {
                        w.putObject(o);
                    }
                }
            };
        }

        private static class Info implements TaskInfo {
            Object[] values;
            public Info(Object[] values) {
                this.values = values;
            }
        }
    }

    /**
     * value consumer.
     */
    public static class ObjectConsumerProcessor implements VertexProcessor {

        static final Set<Object> RECEIVED = Collections.synchronizedSet(new HashSet<>());

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return c -> {
                try (ObjectReader r = (ObjectReader) c.getInput("in")) {
                    while (r.nextObject()) {
                        RECEIVED.add(r.getObject());
                    }
                }
            };
        }
    }

    /**
     * value consumer.
     */
    public static class GroupConsumerProcessor implements VertexProcessor {

        static final Set<List<Object>> RECEIVED = Collections.synchronizedSet(new HashSet<>());

        @Override
        public TaskProcessor createTaskProcessor() throws IOException, InterruptedException {
            return c -> {
                try (GroupReader r = (GroupReader) c.getInput("in")) {
                    while (r.nextGroup()) {
                        List<Object> values = new ArrayList<>();
                        while (r.nextObject()) {
                            values.add(r.getObject());
                        }
                        RECEIVED.add(values);
                    }
                }
            };
        }
    }
}
