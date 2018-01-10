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
package com.asakusafw.m3bp.mirror.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.FlowGraphMirror;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * An abstract implementation of {@link FlowGraphMirror}.
 */
public abstract class AbstractFlowGraphMirror implements FlowGraphMirror {

    private final List<VertexMirror> vertices = new ArrayList<>();

    @Override
    public List<VertexMirror> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    @Override
    public VertexMirror addVertex(String name, M3bpVertexDescriptor descriptor) {
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(descriptor);
        Identifier id = new Identifier(vertices.size());
        VertexMirror vertex = createVertex(id, name, descriptor);
        vertices.add(vertex);
        return vertex;
    }

    /**
     * Creates a new {@link VertexMirror}.
     * @param id the vertex ID
     * @param name the vertex name
     * @param descriptor the descriptor
     * @return the created mirror
     */
    protected abstract VertexMirror createVertex(Identifier id, String name, M3bpVertexDescriptor descriptor);

    @Override
    public void addEdge(PortMirror upstream, PortMirror downstream) {
        if (upstream instanceof AbstractPortMirror && downstream instanceof AbstractPortMirror) {
            AbstractPortMirror.connect((AbstractPortMirror) upstream, (AbstractPortMirror) downstream);
        }
        internalAddEdge(upstream, downstream);
    }

    /**
     * Adds an edge internally.
     * @param upstream the upstream port
     * @param downstream the downstream port
     */
    protected void internalAddEdge(PortMirror upstream, PortMirror downstream) {
        return;
    }
}
