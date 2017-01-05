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

/**
 * Represents an identifier.
 */
public final class Identifier {

    private final long value;

    /**
     * Creates a new instance.
     * @param value the identifier value
     */
    public Identifier(long value) {
        this.value = value;
    }

    /**
     * Returns the value.
     * @return the value
     */
    public long getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
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
        Identifier other = (Identifier) obj;
        if (value != other.value) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Long.toString(value, 16);
    }
}
