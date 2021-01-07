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

import java.text.MessageFormat;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.FlowGraphMirror;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.m3bp.mirror.basic.AbstractFlowGraphMirror;

/**
 * JNI bridge of {@link FlowGraphMirror}.
 */
public class FlowGraphMirrorImpl extends AbstractFlowGraphMirror implements NativeMirror {

    private final Pointer reference;

    FlowGraphMirrorImpl(Pointer reference) {
        this.reference = reference;
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    protected VertexMirror createVertex(Identifier id, String name, M3bpVertexDescriptor descriptor) {
        Arguments.requireNonNull(id);
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(descriptor);
        Pointer ref = new Pointer(addVertex0(getPointer().getAddress(), name));
        VertexMirror vertex = new VertexMirrorImpl(ref, name, descriptor);
        return vertex;
    }

    @Override
    protected void internalAddEdge(PortMirror upstream, PortMirror downstream) {
        Arguments.requireNonNull(upstream);
        Arguments.requireNonNull(downstream);
        Pointer up = NativeMirror.getPointer(upstream);
        Pointer down = NativeMirror.getPointer(downstream);
        addEdge0(getPointer().getAddress(), up.getAddress(), down.getAddress());
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "FlowGraphMirror[{0}](vertices={1})", //$NON-NLS-1$
                getPointer(),
                getVertices().size());
    }

    private static native long addVertex0(long self, String name);

    private static native void addEdge0(long self, long upstream, long downstream);
}
