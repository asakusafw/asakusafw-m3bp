/**
 * Copyright 2011-2017 Asakusa Framework Team.
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
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.processor.TaskProcessor;
import com.asakusafw.dag.api.processor.TaskProcessorContext;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.InterruptibleIo.Closer;
import com.asakusafw.lang.utils.common.RunnableWithException;

/**
 * Executes tasks.
 */
class TaskExecutor implements RunnableWithException<Exception> {

    static final Logger LOG = LoggerFactory.getLogger(TaskExecutor.class);

    private final VertexProcessor vertexProcessor;

    private final BlockingQueue<? extends TaskProcessorContext> queue;

    /**
     * Creates a new instance.
     * @param processor the parent vertex processor
     * @param queue the task queue
     */
    TaskExecutor(VertexProcessor processor, BlockingQueue<? extends TaskProcessorContext> queue) {
        Arguments.requireNonNull(processor);
        Arguments.requireNonNull(queue);
        this.vertexProcessor = processor;
        this.queue = queue;
    }

    @Override
    public void run() throws IOException, InterruptedException {
        try (Closer closer = new Closer()) {
            TaskProcessor taskProcessor = null;
            while (true) {
                TaskProcessorContext next = queue.poll();
                if (next == null) {
                    break;
                }
                if (taskProcessor == null) {
                    LOG.debug("initializing task processor: ({})", vertexProcessor);
                    taskProcessor = closer.add(vertexProcessor.createTaskProcessor());
                }
                LOG.debug("starting task: {} ({})", next.getTaskId(), vertexProcessor);
                taskProcessor.run(next);
            }
        }
    }
}
