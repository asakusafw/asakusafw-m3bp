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
package com.asakusafw.m3bp.mirror.basic;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * A basic implementation of {@link VertexMirror}.
 */
public class BasicVertexMirror extends AbstractVertexMirror {

    private final String name;

    private final M3bpVertexDescriptor descriptor;

    /**
     * Creates a new instance.
     * @param name the vertex name
     * @param descriptor the vertex descriptor
     */
    public BasicVertexMirror(String name, M3bpVertexDescriptor descriptor) {
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(descriptor);
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected M3bpVertexDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    protected PortMirror createInput(
            Identifier portId, String portName, String portTag, M3bpEdgeDescriptor portDescriptor) {
        return new BasicPortMirror(this, portId, portName, portTag, portDescriptor);
    }

    @Override
    protected PortMirror createOutput(
            Identifier portId, String portName, String portTag, M3bpEdgeDescriptor portDescriptor) {
        return new BasicPortMirror(this, portId, portName, portTag, portDescriptor);
    }
}
