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
package com.asakusafw.m3bp.mirror;

import java.util.List;
import java.util.NoSuchElementException;

import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;

/**
 * A mirror of M3BP {@code Vertex}.
 */
public interface VertexMirror {

    /**
     * Returns the vertex name.
     * @return the vertex name
     */
    String getName();

    /**
     * Returns the input ports.
     * @return the ports
     */
    List<? extends PortMirror> getInputs();

    /**
     * Returns an input port.
     * @param name the target port name
     * @return the target port
     * @throws NoSuchElementException if there is no such a port
     */
    default PortMirror getInput(String name) {
        return getInputs().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst().orElseThrow(() -> new NoSuchElementException(name));
    }

    /**
     * Returns the output ports.
     * @return the ports
     */
    List<? extends PortMirror> getOutputs();

    /**
     * Returns an output port.
     * @param name the target port name
     * @return the target port
     * @throws NoSuchElementException if there is no such a port
     */
    default PortMirror getOutput(String name) {
        return getOutputs().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst().orElseThrow(() -> new NoSuchElementException(name));
    }

    /**
     * Creates and returns a new {@link VertexProcessor} instance.
     * @param loader the target class loader
     * @return the created instance
     */
    VertexProcessor newProcessor(ClassLoader loader);

    /**
     * Returns the number of max concurrency.
     * @return the number of max concurrency, or {@code -1} if it is not defined
     */
    default int getMaxConcurrency() {
        return -1;
    }

    /**
     * Adds an input port.
     * @param name the port name
     * @param descriptor the edge descriptor which corresponding to the port
     * @return the created port
     */
    PortMirror addInput(String name, M3bpEdgeDescriptor descriptor);

    /**
     * Adds an output port.
     * @param name the port name
     * @param descriptor the edge descriptor which corresponding to the port
     * @return the created port
     */
    PortMirror addOutput(String name, M3bpEdgeDescriptor descriptor);
}
