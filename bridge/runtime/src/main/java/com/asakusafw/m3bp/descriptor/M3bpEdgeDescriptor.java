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
package com.asakusafw.m3bp.descriptor;

import java.util.Objects;

import com.asakusafw.dag.api.common.KeyValueSerDe;
import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.common.ValueSerDe;
import com.asakusafw.dag.api.model.EdgeDescriptor;
import com.asakusafw.dag.api.model.basic.BasicEdgeDescriptor;
import com.asakusafw.lang.utils.common.Arguments;

/**
 * An implementation of {@link EdgeDescriptor} for M3BP.
 */
public class M3bpEdgeDescriptor extends BasicEdgeDescriptor {

    private static final long serialVersionUID = 2L;

    private final String valueComparatorName;

    /**
     * Creates a new instance.
     * @param movement the movement type
     * @param serde the supplier of {@link ValueSerDe} or {@link KeyValueSerDe}
     * @param comparator the pure-Java value comparator (nullable)
     * @param valueComparatorName the value comparator function name (nullable)
     */
    public M3bpEdgeDescriptor(
            Movement movement, SupplierInfo serde,
            SupplierInfo comparator, String valueComparatorName) {
        super(movement, serde, comparator);
        Arguments.require((comparator == null) == (valueComparatorName == null));
        this.valueComparatorName = valueComparatorName;
    }

    /**
     * Returns the value comparator name.
     * @return the value comparator name
     */
    public String getValueComparatorName() {
        return valueComparatorName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hashCode(valueComparatorName);
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
        M3bpEdgeDescriptor other = (M3bpEdgeDescriptor) obj;
        return super.equals(other)
                && Objects.equals(valueComparatorName, other.valueComparatorName);
    }
}
