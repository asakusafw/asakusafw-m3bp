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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import com.asakusafw.dag.api.processor.EdgeReader;
import com.asakusafw.dag.api.processor.EdgeWriter;
import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.api.processor.TaskInfo;
import com.asakusafw.dag.api.processor.TaskProcessor;
import com.asakusafw.dag.api.processor.TaskProcessorContext;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.api.processor.basic.BasicTaskInfo;
import com.asakusafw.dag.utils.common.Optionals;

/**
 * Test for {@link TaskExecutorTest}.
 */
public class TaskExecutorTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        Counter counter = new Counter();
        BlockingQueue<TaskProcessorContext> tasks = tasks(1);
        TaskExecutor exec = new TaskExecutor(counter, tasks);
        exec.run();
        assertThat(counter.processors.get(), is(1));
        assertThat(counter.tasks.get(), is(1));
    }

    /**
     * multiple tasks.
     * @throws Exception if failed
     */
    @Test
    public void multiple_tasks() throws Exception {
        Counter counter = new Counter();
        BlockingQueue<TaskProcessorContext> tasks = tasks(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        TaskExecutor exec = new TaskExecutor(counter, tasks);
        exec.run();
        assertThat(counter.processors.get(), is(1));
        assertThat(counter.tasks.get(), is(55));
    }

    /**
     * empty tasks.
     * @throws Exception if failed
     */
    @Test
    public void empty_tasks() throws Exception {
        Counter counter = new Counter();
        BlockingQueue<TaskProcessorContext> tasks = tasks();
        TaskExecutor exec = new TaskExecutor(counter, tasks);
        exec.run();
        assertThat(counter.processors.get(), is(0));
        assertThat(counter.tasks.get(), is(0));
    }

    private BlockingQueue<TaskProcessorContext> tasks(int... counts) {
        return IntStream.of(counts)
                .mapToObj(MockContext::new)
                .collect(Collectors.toCollection(LinkedBlockingQueue::new));
    }

    private static final class Counter implements VertexProcessor {

        final AtomicInteger tasks = new AtomicInteger();

        final AtomicInteger processors = new AtomicInteger();

        Counter() {
            return;
        }

        @Override
        public TaskProcessor createTaskProcessor() {
            processors.addAndGet(1);
            return c -> c.getTaskInfo()
                    .map(i -> ((Count) i).value)
                    .ifPresent(tasks::addAndGet);
        }
    }

    private static class Count extends BasicTaskInfo {

        final int value;

        Count(int value) {
            this.value = value;
        }
    }

    private static class MockContext implements TaskProcessorContext {

        private final TaskInfo info;

        MockContext(int count) {
            this.info = new Count(count);
        }

        @Override
        public Optional<TaskInfo> getTaskInfo() {
            return Optionals.of(info);
        }

        @Override
        public EdgeReader getInput(String name) throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public EdgeWriter getOutput(String name) throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }

        @Override
        public String getVertexId() {
            return "testing";
        }

        @Override
        public String getTaskId() {
            return String.valueOf(hashCode());
        }

        @Override
        public ProcessorContext getDetached() {
            throw new UnsupportedOperationException();
        }
    }
}
