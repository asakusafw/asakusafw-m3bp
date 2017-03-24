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
package com.asakusafw.m3bp.mirror.jni;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.asakusafw.lang.utils.common.Invariants;

final class NativeBufferUtil {

    private NativeBufferUtil() {
        return;
    }

    static ByteBuffer getView(long ptr, int length) {
        return getView0(ptr, length).order(ByteOrder.nativeOrder());
    }

    static long getAddress(ByteBuffer buffer) {
        Invariants.require(buffer.isDirect());
        return getAddress0(buffer);
    }

    static int compare(long aPtr, long bPtr, long length) {
        return compare0(aPtr, bPtr, length);
    }

    private static native ByteBuffer getView0(long ptr, int length);

    private static native long getAddress0(ByteBuffer buffer);

    private static native int compare0(long aPtr, long bPtr, long length);
}
