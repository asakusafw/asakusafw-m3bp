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
package com.asakusafw.m3bp.mirror.jna;

import java.nio.ByteBuffer;

import com.asakusafw.lang.utils.common.Arguments;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Compare buffers.
 */
@FunctionalInterface
public interface BufferComparator {

    /**
     * Compares between two contents.
     * @param a pointer to the first contents
     * @param b pointer to the second contents
     * @return {@code true} iff {@code a < b}
     */
    boolean compare(Pointer a, Pointer b);

    /**
     * Compares between two contents.
     * @param a pointer to the first contents (must be a direct buffer)
     * @param b pointer to the second contents (must be a direct buffer)
     * @return {@code true} iff {@code a < b}
     */
    default boolean compare(ByteBuffer a, ByteBuffer b) {
        Arguments.requireNonNull(a);
        Arguments.requireNonNull(b);
        Pointer pa = Native.getDirectBufferPointer(a);
        Pointer pb = Native.getDirectBufferPointer(b);
        return compare(Util.add(pa, a.position()), Util.add(pb, b.position()));
    }
}
