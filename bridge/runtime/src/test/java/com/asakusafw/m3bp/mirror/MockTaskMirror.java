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
package com.asakusafw.m3bp.mirror;

import java.util.NoSuchElementException;

/**
 * Mock {@link TaskMirror}.
 */
public class MockTaskMirror implements TaskMirror {

    private final EdgeIoProvider provider;

    private final VertexMirror vertex;

    private final Identifier taskId;

    /**
     * Creates a new instance.
     * @param provider the I/O provider
     * @param vertex the target vertex
     * @param taskId the task ID
     */
    public MockTaskMirror(EdgeIoProvider provider, VertexMirror vertex, Identifier taskId) {
        this.provider = provider;
        this.vertex = vertex;
        this.taskId = taskId;
    }

    @Override
    public Identifier logicalTaskId() {
        return taskId;
    }

    @Override
    public Identifier phisicalTaskId() {
        return taskId;
    }

    @Override
    public InputReaderMirror input(Identifier id) {
        PortMirror port = vertex.getInputs().stream()
                .filter(p -> p.getId().equals(id))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException());
        return provider.getInput(vertex.getName(), port.getName());
    }

    @Override
    public OutputWriterMirror output(Identifier id) {
        PortMirror port = vertex.getOutputs().stream()
                .filter(p -> p.getId().equals(id))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException());
        return provider.getOutput(vertex.getName(), port.getName());
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
