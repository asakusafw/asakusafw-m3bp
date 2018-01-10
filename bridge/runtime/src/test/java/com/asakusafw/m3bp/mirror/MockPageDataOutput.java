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
package com.asakusafw.m3bp.mirror;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.stream.LongStream;

import com.asakusafw.m3bp.mirror.basic.AbstractPageDataOutput;

/**
 * Mock {@link PageDataOutput}.
 */
public class MockPageDataOutput extends AbstractPageDataOutput {

    /**
     * Creates a new instance.
     */
    public MockPageDataOutput() {
        reset(100, 1024, 64);
    }

    /**
     * Creates a new instance.
     * @param flushFactor the flush factor in contents filling ratio
     * @param offset the entry base offset address
     * @param bufferSize the buffer size in bytes
     * @param maxRecords the max record count
     */
    public MockPageDataOutput(float flushFactor, long offset, int bufferSize, int maxRecords) {
        super(flushFactor);
        reset(offset, bufferSize, maxRecords);
    }

    /**
     * Resets the buffer contents.
     * @param offset the address offset
     * @param bufferSize the buffer size
     * @param maxRecords the max record count
     */
    protected void reset(long offset, int bufferSize, int maxRecords) {
        ByteBuffer data = ByteBuffer.allocate(bufferSize);
        ByteBuffer offsets = ByteBuffer.allocate((maxRecords + 1) * Long.BYTES);
        ByteBuffer keys = ByteBuffer.allocate(maxRecords * Long.BYTES);
        reset(offset, data, offsets, keys);
    }

    /**
     * Returns the buffer contents (big-endian).
     * @return the buffer
     */
    public byte[] getContents() {
        return toByteArray(getContentsBuffer(), true);
    }

    /**
     * Returns the entry offsets.
     * @return the entry offsets
     */
    public int[] getPageOffsets() {
        return toIntArray(getEntryOffsetsBuffer(), -getBase(), true);
    }

    /**
     * Returns the key lengths.
     * @return the key lengths
     */
    public int[] getKeyLengths() {
        return toIntArray(getKeyLengthsBuffer(), 0, true);
    }

    /**
     * Returns the buffer contents (big-endian) (only from {@link #doFlush(boolean)} method).
     * @return the buffer
     */
    protected byte[] getContentsInFlush() {
        return toByteArray(getContentsBuffer(), false);
    }

    /**
     * Returns the entry offsets (only from {@link #doFlush(boolean)} method).
     * @return the entry offsets
     */
    protected int[] getPageOffsetsInFlush() {
        return toIntArray(getEntryOffsetsBuffer(), -getBase(), false);
    }

    /**
     * Returns the key lengths (only from {@link #doFlush(boolean)} method).
     * @return the key lengths
     */
    protected int[] getKeyLengthsInFlush() {
        return toIntArray(getKeyLengthsBuffer(), 0, false);
    }

    private byte[] toByteArray(ByteBuffer b, boolean flip) {
        ByteBuffer buf = b.duplicate();
        if (flip) {
            buf.flip();
        }
        byte[] results = new byte[buf.remaining()];
        buf.get(results);
        return results;
    }

    private int[] toIntArray(ByteBuffer b, long delta, boolean flip) {
        ByteBuffer copy = b.duplicate();
        if (flip) {
            copy.flip();
        }
        LongBuffer buf = copy.asLongBuffer();
        long[] results = new long[buf.remaining()];
        buf.get(results);
        return LongStream.of(results).mapToInt(v -> (int) (v + delta)).toArray();
    }
}
