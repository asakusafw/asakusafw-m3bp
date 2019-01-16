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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.api.processor.basic.BasicProcessorContext;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.EdgeIoProvider;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.MockTaskMirror;
import com.asakusafw.m3bp.mirror.TaskMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.m3bp.mirror.basic.BasicVertexMirror;

/**
 * Test for {@link VertexProcessorBridge}.
 */
public class VertexProcessorBridgeTest {

    private static final String NAME = "v";

    /**
     * Disposes all resources.
     */
    @Rule
    public final ExternalResource closer = new ExternalResource() {
        @Override
        protected void after() {
            SupplierWrapper.close();
        }
    };

    private final EdgeIoProvider io = new EdgeIoProvider();

    private int currentTaskId;

    /**
     * Simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        AtomicBoolean saw = new AtomicBoolean();
        VertexMirror vertex = create(() -> context -> {
            assertThat(saw.compareAndSet(false, true), is(true));
        });
        ProcessorContext context = context();
        VertexProcessorBridge bridge = new VertexProcessorBridge(vertex);
        bridge.globalInitialize(context, task(vertex));
        try {
            assertThat(bridge.taskCount(), is(-1));
            assertThat(bridge.maxConcurrency(), is(-1));
            bridge.threadLocalInitialize(context, task(vertex));
            try {
                bridge.run(context, task(vertex));
            } finally {
                bridge.threadLocalFinalize(context, task(vertex));
            }
        } finally {
            bridge.globalFinalize(context, task(vertex));
        }
        assertThat(saw.get(), is(true));
    }

    private BasicProcessorContext context() {
        return new BasicProcessorContext(getClass().getClassLoader());
    }

    private TaskMirror task(VertexMirror mirror) {
        return new MockTaskMirror(io, mirror, new Identifier(currentTaskId++));
    }

    private VertexMirror create(VertexProcessor processor) {
        SupplierWrapper.processor = processor;
        VertexMirror vertex = new BasicVertexMirror(
                NAME,
                new M3bpVertexDescriptor(SupplierInfo.of(SupplierWrapper.class.getName())));
        return vertex;
    }

    /**
     * {@link Supplier} class for testing.
     */
    public static final class SupplierWrapper implements Supplier<VertexProcessor> {

        static VertexProcessor processor;

        @Override
        public VertexProcessor get() {
            return processor;
        }

        static void close() {
            if (processor != null) {
                try {
                    processor.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                processor = null;
            }
        }
    }
}
