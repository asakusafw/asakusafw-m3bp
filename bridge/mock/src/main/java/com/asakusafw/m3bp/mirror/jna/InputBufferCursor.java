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
package com.asakusafw.m3bp.mirror.jna;

import java.util.function.Supplier;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Invariants;

/**
 * A cursor which provides {@link InputBufferFragment}s.
 */
public class InputBufferCursor {

    private final Supplier<? extends InputBufferFragment> keys;

    private final Supplier<? extends InputBufferFragment> values;

    /**
     * Creates a new instance (key and value).
     * @param keys key fragments
     * @param values value fragments
     */
    public InputBufferCursor(
            Supplier<? extends InputBufferFragment> keys,
            Supplier<? extends InputBufferFragment> values) {
        Arguments.requireNonNull(keys);
        Arguments.requireNonNull(values);
        this.keys = keys;
        this.values = values;
    }

    /**
     * Creates a new instance (value-only).
     * @param values value fragments
     */
    public InputBufferCursor(Supplier<? extends InputBufferFragment> values) {
        Arguments.requireNonNull(values);
        this.keys = null;
        this.values = values;
    }

    /**
     * Returns whether key information exists or not.
     * @return {@code true} this key information exists, otherwise {@code false}
     */
    public boolean hasKey() {
        return keys != null;
    }

    /**
     * Returns the next key buffer fragment.
     * @return the next key buffer fragment, or {@code null} if there are no more keys
     * @see #hasKey()
     */
    public InputBufferFragment nextKey() {
        Invariants.requireNonNull(keys);
        return keys.get();
    }

    /**
     * Returns the next value buffer fragment.
     * @return the next value buffer fragment, or {@code null} if there are no more ßs
     */
    public InputBufferFragment nextValue() {
        return values.get();
    }
}
