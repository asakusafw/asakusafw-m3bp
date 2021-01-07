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
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.Movement;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;
import com.asakusafw.m3bp.mirror.basic.AbstractVertexMirror;

/**
 * JNI bridge of {@link VertexMirror}.
 */
public class VertexMirrorImpl extends AbstractVertexMirror implements NativeMirror {

    private final String name;

    private final Pointer reference;

    private final M3bpVertexDescriptor descriptor;

    VertexMirrorImpl(Pointer reference, String name, M3bpVertexDescriptor descriptor) {
        Arguments.requireNonNull(reference);
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(descriptor);
        this.name = name;
        this.reference = reference;
        this.descriptor = descriptor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    protected M3bpVertexDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    protected PortMirror createInput(
            Identifier portId, String portName, String portTag, M3bpEdgeDescriptor portDescriptor) {
        Pointer ref = new Pointer(createInput0(
                getPointer().getAddress(),
                portId.getValue(),
                portName,
                Movement.of(portDescriptor.getMovement()).getId(),
                portDescriptor.getValueComparatorName()));
        PortMirror port = new PortMirrorImpl(ref, portId, portName, portTag, this, portDescriptor);
        return port;
    }

    @Override
    protected PortMirror createOutput(
            Identifier portId, String portName, String portTag, M3bpEdgeDescriptor portDescriptor) {
        Pointer ref = new Pointer(createOutput0(
                getPointer().getAddress(),
                portId.getValue(),
                portName,
                Movement.of(portDescriptor.getMovement()) == Movement.SCATTER_GATHER));
        PortMirror port = new PortMirrorImpl(ref, portId, portName, portTag, this, portDescriptor);
        return port;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "VertexMirror[{0}](name={1})", //$NON-NLS-1$
                getPointer(),
                getName());
    }

    private static native long createInput0(long address,
            long portId, String portName, int movementType, String comparatorName);

    private static native long createOutput0(long address,
            long portId, String portName, boolean hasKey);
}
