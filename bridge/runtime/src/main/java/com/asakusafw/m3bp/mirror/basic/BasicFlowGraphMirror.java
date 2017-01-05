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

import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.FlowGraphMirror;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * A basic implementation of {@link FlowGraphMirror}.
 */
public class BasicFlowGraphMirror extends AbstractFlowGraphMirror {

    @Override
    protected VertexMirror createVertex(Identifier id, String name, M3bpVertexDescriptor descriptor) {
        return new BasicVertexMirror(name, descriptor);
    }
}
