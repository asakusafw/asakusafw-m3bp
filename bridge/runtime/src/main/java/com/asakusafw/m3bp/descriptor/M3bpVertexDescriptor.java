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
package com.asakusafw.m3bp.descriptor;

import java.text.MessageFormat;
import java.util.Objects;

import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.model.VertexDescriptor;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.lang.utils.common.Arguments;

/**
 * An implementation of {@link VertexDescriptor} for M3BP.
 */
public class M3bpVertexDescriptor implements VertexDescriptor {

    private static final long serialVersionUID = 1L;

    private final SupplierInfo vertexProcessor;

    /**
     * Creates a new instance.
     * @param vertexProcessor a {@link VertexProcessor} provider
     */
    public M3bpVertexDescriptor(SupplierInfo vertexProcessor) {
        Arguments.requireNonNull(vertexProcessor);
        this.vertexProcessor = vertexProcessor;
    }

    /**
     * Returns supplier information of target {@link VertexProcessor}.
     * @return the vertex processor provider
     */
    public SupplierInfo getVertexProcessor() {
        return vertexProcessor;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(vertexProcessor);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        M3bpVertexDescriptor other = (M3bpVertexDescriptor) obj;
        if (!Objects.equals(vertexProcessor, other.vertexProcessor)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "Vertex({0})", //$NON-NLS-1$
                vertexProcessor);
    }
}
