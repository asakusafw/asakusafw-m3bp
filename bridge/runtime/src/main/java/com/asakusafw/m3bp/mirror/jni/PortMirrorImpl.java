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
package com.asakusafw.m3bp.mirror.jni;

import java.text.MessageFormat;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.basic.AbstractPortMirror;

/**
 * JNI bridge of {@link PortMirror}.
 */
public class PortMirrorImpl extends AbstractPortMirror implements NativeMirror {

    private final Pointer reference;

    private final Identifier id;

    private final String name;

    private final String tag;

    private final VertexMirrorImpl owner;

    private final M3bpEdgeDescriptor descriptor;

    PortMirrorImpl(
            Pointer reference,
            Identifier id, String name, String tag,
            VertexMirrorImpl owner, M3bpEdgeDescriptor descriptor) {
        Arguments.requireNonNull(reference);
        Arguments.requireNonNull(id);
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(owner);
        Arguments.requireNonNull(descriptor);
        this.reference = reference;
        this.id = id;
        this.tag = tag;
        this.name = name;
        this.owner = owner;
        this.descriptor = descriptor;
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public VertexMirrorImpl getOwner() {
        return owner;
    }

    @Override
    protected M3bpEdgeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "PortMirror[{0}](name={1})", //$NON-NLS-1$
                getPointer(),
                getName());
    }
}
