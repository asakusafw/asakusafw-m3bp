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
package com.asakusafw.m3bp.mirror;

import java.util.List;
import java.util.NoSuchElementException;

import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;

/**
 * A mirror of M3BP {@code Vertex}.
 * @since 0.1.0
 * @version 0.2.0
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
     * Adds an input port.
     * @param name the port name
     * @param descriptor the edge descriptor which corresponding to the port
     * @return the created port
     */
    default PortMirror addInput(String name, M3bpEdgeDescriptor descriptor) {
        return addInput(name, null, descriptor);
    }

    /**
     * Adds an input port.
     * @param name the port name
     * @param tag the optional port tag (nullable)
     * @param descriptor the edge descriptor which corresponding to the port
     * @return the created port
     * @since 0.2.0
     */
    PortMirror addInput(String name, String tag, M3bpEdgeDescriptor descriptor);

    /**
     * Adds an output port.
     * @param name the port name
     * @param descriptor the edge descriptor which corresponding to the port
     * @return the created port
     */
    default PortMirror addOutput(String name, M3bpEdgeDescriptor descriptor) {
        return addOutput(name, null, descriptor);
    }

    /**
     * Adds an output port.
     * @param name the port name
     * @param tag the optional port tag (nullable)
     * @param descriptor the edge descriptor which corresponding to the port
     * @return the created port
     * @since 0.2.0
     */
    PortMirror addOutput(String name, String tag, M3bpEdgeDescriptor descriptor);
}
