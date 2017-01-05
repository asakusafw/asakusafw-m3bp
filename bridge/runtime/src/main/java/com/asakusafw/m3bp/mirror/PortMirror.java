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
package com.asakusafw.m3bp.mirror;

import java.util.Set;

import com.asakusafw.dag.api.common.KeyValueSerDe;
import com.asakusafw.dag.api.common.ValueSerDe;

/**
 * A mirror of M3BP {@code InputPort} and {@code OutputPort}.
 * @since 0.1.0
 * @version 0.2.0
 */
public interface PortMirror {

    /**
     * Returns the port ID.
     * @return the port ID
     */
    Identifier getId();

    /**
     * Returns the name.
     * @return the name
     */
    String getName();

    /**
     * Returns the port tag.
     * @return the port tag, or {@code null} if it is not defined
     * @since 0.2.0
     */
    default String getTag() {
        return null;
    }

    /**
     * Returns the owner.
     * @return the owner
     */
    VertexMirror getOwner();

    /**
     * Returns the connected opposites.
     * @return the connected opposites
     */
    Set<? extends PortMirror> getOpposites();

    /**
     * Creates a new value ser/de.
     * @param loader the class loader
     * @return the created ser/de
     */
    ValueSerDe newValueSerDe(ClassLoader loader);

    /**
     * Creates a new key-value ser/de.
     * @param loader the class loader
     * @return the created ser/de
     */
    KeyValueSerDe newKeyValueSerDe(ClassLoader loader);

    /**
     * Returns the value comparator function name.
     * @return the function name
     */
    String getValueComparatorName();

    /**
     * Returns whether this port has key or not.
     * @return {@code true} if this port has key, otherwise {@code false}
     */
    boolean hasKey();

    /**
     * Returns whether this port has value or not.
     * @return {@code true} if this port has value, otherwise {@code false}
     */
    boolean hasValue();

    /**
     * Returns the movement type.
     * @return the movement type
     */
    Movement getMovement();
}
