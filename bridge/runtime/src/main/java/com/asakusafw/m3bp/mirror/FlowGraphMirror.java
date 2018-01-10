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

import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;

/**
 * A mirror of M3BP {@code FlowGraph}.
 */
public interface FlowGraphMirror {

    /**
     * Drives a graph information.
     * @param graph the target graph
     */
    default void drive(GraphInfo graph) {
        FlowGraphDriver.drive(this, graph);
    }

    /**
     * Returns the registered vertices.
     * @return the vertices
     */
    List<? extends VertexMirror> getVertices();

    /**
     * Adds a new vertex.
     * @param name the vertex name
     * @param descriptor the descriptor
     * @return the created vertex
     */
    VertexMirror addVertex(String name, M3bpVertexDescriptor descriptor);

    /**
     * Adds an edge.
     * @param upstream the upstream port
     * @param downstream the downstream port
     */
    void addEdge(PortMirror upstream, PortMirror downstream);
}
