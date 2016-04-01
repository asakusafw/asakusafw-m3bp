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

import com.asakusafw.dag.api.common.KeyValueSerDe;
import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.common.ValueSerDe;
import com.asakusafw.dag.api.model.EdgeDescriptor;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.Movement;

/**
 * An implementation of {@link EdgeDescriptor} for M3BP.
 */
public class M3bpEdgeDescriptor implements EdgeDescriptor {

    private static final long serialVersionUID = 1L;

    private final Movement movement;

    private final SupplierInfo serde;

    private final String valueComparatorName;

    /**
     * Creates a new instance.
     * @param movement the movement type
     * @param serde the supplier of {@link ValueSerDe} or {@link KeyValueSerDe}
     * @param valueComparatorName the value comparator function name
     */
    public M3bpEdgeDescriptor(
            Movement movement,
            SupplierInfo serde,
            String valueComparatorName) {
        Arguments.requireNonNull(movement);
        switch (movement) {
        case ONE_TO_ONE:
        case BROADCAST:
            Arguments.require(serde != null);
            Arguments.require(valueComparatorName == null);
            break;
        case SCATTER_GATHER:
            Arguments.require(serde != null);
            break;
        case NOTHING:
            Arguments.require(serde == null);
            Arguments.require(valueComparatorName == null);
            break;
        default:
            throw new AssertionError();
        }
        this.movement = movement;
        this.serde = serde;
        this.valueComparatorName = valueComparatorName;
    }

    /**
     * Returns the movement.
     * @return the movement
     */
    public Movement getMovement() {
        return movement;
    }

    /**
     * Returns the supplier of {@link ValueSerDe} or {@link KeyValueSerDe}.
     * @return the ser/de
     */
    public SupplierInfo getSerDe() {
        return serde;
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
        int result = 1;
        result = prime * result + Objects.hashCode(movement);
        result = prime * result + Objects.hashCode(serde);
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
        if (!Objects.equals(movement, other.movement)) {
            return false;
        }
        if (!Objects.equals(serde, other.serde)) {
            return false;
        }
        if (!Objects.equals(valueComparatorName, other.valueComparatorName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "Edge({0})", //$NON-NLS-1$
                movement.name());
    }
}
