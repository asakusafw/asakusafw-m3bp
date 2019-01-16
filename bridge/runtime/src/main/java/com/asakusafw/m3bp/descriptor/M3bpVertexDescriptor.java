/**
 * Copyright 2011-2019 Asakusa Framework Team.
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
package com.asakusafw.m3bp.descriptor;

import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.model.VertexDescriptor;
import com.asakusafw.dag.api.model.basic.BasicVertexDescriptor;
import com.asakusafw.dag.api.processor.VertexProcessor;

/**
 * An implementation of {@link VertexDescriptor} for M3BP.
 */
public class M3bpVertexDescriptor extends BasicVertexDescriptor {

    private static final long serialVersionUID = 2L;

    /**
     * Creates a new instance.
     * @param vertexProcessor a {@link VertexProcessor} provider
     */
    public M3bpVertexDescriptor(SupplierInfo vertexProcessor) {
        super(vertexProcessor);
    }
}
