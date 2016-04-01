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
package com.asakusafw.m3bp.mirror;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.IntStream;

import com.asakusafw.m3bp.mirror.basic.AbstractPageDataInput;

/**
 * Mock {@link PageDataInput}.
 */
public class MockPageDataInput extends AbstractPageDataInput {

    /**
     * Creates a new instance.
     * @param contents the contents (must be big-endian)
     * @param offsets the offset table
     */
    public MockPageDataInput(byte[] contents, int[] offsets) {
        long offset = 0x1234;
        reset(offset, contents, offsets);
    }

    /**
     * Resets the contents.
     * @param offset the base address
     * @param data the contents
     * @param offsets the offsets
     */
    protected void reset(long offset, byte[] data, int[] offsets) {
        ByteBuffer contentsBuf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer offsetsBuf = ByteBuffer.allocate(offsets.length * Long.BYTES)
            .order(ByteOrder.BIG_ENDIAN);
        offsetsBuf.asLongBuffer()
            .put(IntStream.of(offsets).asLongStream().map(v -> v + offset).toArray());
        reset(offset, contentsBuf, offsetsBuf);
    }
}
