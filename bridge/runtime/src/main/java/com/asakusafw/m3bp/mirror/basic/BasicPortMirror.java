/**
 * Copyright 2011-2017 Asakusa Framework Team.
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
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * A basic implementation of {@link PortMirror}.
 * @since 0.1.0
 * @version 0.2.0
 */
public class BasicPortMirror extends AbstractPortMirror {

    private final VertexMirror owner;

    private final Identifier id;

    private final String name;

    private final String tag;

    private final M3bpEdgeDescriptor descriptor;

    /**
     * Creates a new instance.
     * @param owner the owner
     * @param id the port ID
     * @param name the port name
     * @param descriptor the descriptor
     */
    public BasicPortMirror(VertexMirror owner, Identifier id, String name, M3bpEdgeDescriptor descriptor) {
        this(owner, id, name, null, descriptor);
    }

    /**
     * Creates a new instance.
     * @param owner the owner
     * @param id the port ID
     * @param name the port name
     * @param tag the optional port tag (nullable)
     * @param descriptor the descriptor
     */
    public BasicPortMirror(VertexMirror owner, Identifier id, String name, String tag, M3bpEdgeDescriptor descriptor) {
        Arguments.requireNonNull(owner);
        Arguments.requireNonNull(id);
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(descriptor);
        this.owner = owner;
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.descriptor = descriptor;
    }

    @Override
    public VertexMirror getOwner() {
        return owner;
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
    protected M3bpEdgeDescriptor getDescriptor() {
        return descriptor;
    }
}
