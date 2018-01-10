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
package com.asakusafw.m3bp.descriptor;

import com.asakusafw.dag.api.common.DataComparator;
import com.asakusafw.dag.api.common.KeyValueSerDe;
import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.common.ValueSerDe;
import com.asakusafw.dag.api.model.EdgeDescriptor;
import com.asakusafw.dag.api.model.VertexDescriptor;
import com.asakusafw.dag.api.model.basic.BasicEdgeDescriptor.Movement;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Optionals;

/**
 * Provides descriptors for operations on M3BP.
 */
public final class Descriptors {

    private Descriptors() {
        return;
    }

    /**
     * Creates a new {@link VertexDescriptor}.
     * @param processor the {@link VertexProcessor} class
     * @return the created descriptor
     */
    public static M3bpVertexDescriptor newVertex(Class<? extends VertexProcessor> processor) {
        Arguments.requireNonNull(processor);
        return newVertex(SupplierInfo.of(processor.getName()));
    }

    /**
     * Creates a new {@link VertexDescriptor}.
     * @param processor the {@link VertexProcessor} class
     * @return the created descriptor
     */
    public static M3bpVertexDescriptor newVertex(SupplierInfo processor) {
        Arguments.requireNonNull(processor);
        return new M3bpVertexDescriptor(processor);
    }

    /**
     * Creates a new void {@link EdgeDescriptor}.
     * @return the created descriptor
     */
    public static M3bpEdgeDescriptor newVoidEdge() {
        return new M3bpEdgeDescriptor(Movement.NOTHING, null, null, null);
    }

    /**
     * Creates a new one-to-one {@link EdgeDescriptor}.
     * @param serde the ser/de class
     * @return the created descriptor
     */
    public static M3bpEdgeDescriptor newOneToOneEdge(Class<? extends ValueSerDe> serde) {
        Arguments.requireNonNull(serde);
        return newOneToOneEdge(SupplierInfo.of(serde.getName()));
    }

    /**
     * Creates a new one-to-one {@link EdgeDescriptor}.
     * @param serde the ser/de class
     * @return the created descriptor
     */
    public static M3bpEdgeDescriptor newOneToOneEdge(SupplierInfo serde) {
        Arguments.requireNonNull(serde);
        return new M3bpEdgeDescriptor(Movement.ONE_TO_ONE, serde, null, null);
    }

    /**
     * Creates a new broadcast {@link EdgeDescriptor}.
     * @param serde the ser/de class
     * @return the created descriptor
     */
    public static M3bpEdgeDescriptor newBroadcastEdge(Class<? extends ValueSerDe> serde) {
        Arguments.requireNonNull(serde);
        return newBroadcastEdge(SupplierInfo.of(serde.getName()));
    }

    /**
     * Creates a new broadcast {@link EdgeDescriptor}.
     * @param serde the ser/de class
     * @return the created descriptor
     */
    public static M3bpEdgeDescriptor newBroadcastEdge(SupplierInfo serde) {
        Arguments.requireNonNull(serde);
        return new M3bpEdgeDescriptor(Movement.BROADCAST, serde, null, null);
    }

    /**
     * Creates a new scatter-gather {@link EdgeDescriptor}.
     * @param serde the ser/de class
     * @param comparator the Java value comparator class
     * @param comparatorName the native value comparator function name
     * @return the created descriptor
     */
    public static M3bpEdgeDescriptor newScatterGatherEdge(
            Class<? extends KeyValueSerDe> serde,
            Class<? extends DataComparator> comparator,
            String comparatorName) {
        Arguments.requireNonNull(serde);
        return newScatterGatherEdge(
                SupplierInfo.of(serde.getName()),
                Optionals.of(comparator)
                    .map(Class::getName)
                    .map(SupplierInfo::of)
                    .orElse(null),
                comparatorName);
    }

    /**
     * Creates a new scatter-gather {@link EdgeDescriptor}.
     * @param serde the ser/de class
     * @param comparator the Java value comparator class
     * @param comparatorName the native value comparator function name
     * @return the created descriptor
     */
    public static M3bpEdgeDescriptor newScatterGatherEdge(
            SupplierInfo serde, SupplierInfo comparator, String comparatorName) {
        Arguments.requireNonNull(serde);
        return new M3bpEdgeDescriptor(Movement.SCATTER_GATHER, serde, comparator, comparatorName);
    }
}
