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
package com.asakusafw.m3bp.compiler.core;

import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.model.EdgeDescriptor;
import com.asakusafw.dag.api.model.VertexDescriptor;
import com.asakusafw.dag.compiler.codegen.ClassGeneratorContext;
import com.asakusafw.dag.compiler.codegen.KeyValueSerDeGenerator;
import com.asakusafw.dag.compiler.codegen.ValueSerDeGenerator;
import com.asakusafw.dag.compiler.flow.DagDescriptorFactory;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.description.TypeDescription;
import com.asakusafw.lang.compiler.model.graph.Group;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.compiler.comparator.NativeValueComparatorExtension;
import com.asakusafw.m3bp.descriptor.Descriptors;

/**
 * Provides descriptors of DAG API.
 * @since 0.2.0
 */
public class M3bpDescriptorFactory implements DagDescriptorFactory {

    private final ClassGeneratorContext context;

    private final NativeValueComparatorExtension nativeComparators;

    /**
     * Creates a new instance.
     * @param context the current context
     * @param nativeComparators the native value comparator provider
     */
    public M3bpDescriptorFactory(ClassGeneratorContext context, NativeValueComparatorExtension nativeComparators) {
        Arguments.requireNonNull(context);
        Arguments.requireNonNull(nativeComparators);
        this.context = context;
        this.nativeComparators = nativeComparators;
    }

    @Override
    public VertexDescriptor newVertex(ClassDescription processor) {
        return Descriptors.newVertex(toSupplier(processor));
    }

    @Override
    public EdgeDescriptor newVoidEdge() {
        return Descriptors.newVoidEdge();
    }

    @Override
    public EdgeDescriptor newOneToOneEdge(TypeDescription dataType, ClassDescription serde) {
        Arguments.requireNonNull(dataType);
        Arguments.requireNonNull(serde);
        return Descriptors.newOneToOneEdge(toSupplier(serde));
    }

    @Override
    public EdgeDescriptor newBroadcastEdge(TypeDescription dataType, ClassDescription serde) {
        Arguments.requireNonNull(dataType);
        Arguments.requireNonNull(serde);
        return Descriptors.newBroadcastEdge(toSupplier(serde));
    }

    @Override
    public EdgeDescriptor newScatterGatherEdge(TypeDescription dataType, ClassDescription serde, Group group) {
        Arguments.requireNonNull(dataType);
        Arguments.requireNonNull(group);
        String comparatorName = nativeComparators.addComparator(dataType, group);
        return Descriptors.newScatterGatherEdge(toSupplier(serde), comparatorName);
    }

    @Override
    public EdgeDescriptor newOneToOneEdge(TypeDescription dataType) {
        Arguments.requireNonNull(dataType);
        ClassDescription serde = ValueSerDeGenerator.get(context, dataType);
        return newOneToOneEdge(dataType, serde);
    }

    @Override
    public EdgeDescriptor newBroadcastEdge(TypeDescription dataType) {
        Arguments.requireNonNull(dataType);
        ClassDescription serde = ValueSerDeGenerator.get(context, dataType);
        return newBroadcastEdge(dataType, serde);
    }

    @Override
    public EdgeDescriptor newScatterGatherEdge(TypeDescription dataType, Group group) {
        Arguments.requireNonNull(dataType);
        Arguments.requireNonNull(group);
        ClassDescription serde = KeyValueSerDeGenerator.get(context, dataType, group);
        return newScatterGatherEdge(dataType, serde, group);
    }

    private static SupplierInfo toSupplier(ClassDescription aClass) {
        return SupplierInfo.of(aClass.getBinaryName());
    }
}
