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
package com.asakusafw.m3bp.mirror.unsafe;

import java.io.IOException;
import java.nio.ByteOrder;

import com.asakusafw.m3bp.mirror.PageDataInput;

/**
 * Unsafe implementation of {@link PageDataInput}.
 */
@SuppressWarnings("restriction")
public class UnsafePageDataInput extends UnsafeDataBuffer implements PageDataInput {

    private static final int SMALL_PAGE_THRESHOLD = Long.BYTES * 4;

    private static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    private long basePtr = 0L;

    private long tablePtr = 0L;

    private long tableEnd = 0L;

    private long dataBegin = 0L;

    private long dataEnd = 0L;

    @Override
    public boolean next() throws IOException {
        while (true) {
            if (advanceOnTable()) {
                return true;
            }
            if (advance() == false) {
                return false;
            }
        }
    }

    private boolean advanceOnTable() {
        if (Long.compareUnsigned(tablePtr, tableEnd) >= 0) {
            return false;
        }
        long currentEnd = dataEnd;
        long nextEnd = basePtr + UNSAFE.getLong(tablePtr);
        tablePtr += Long.BYTES;
        dataBegin = currentEnd;
        dataPtr = currentEnd;
        dataEnd = nextEnd;
        return true;
    }

    /**
     * Advances the table region.
     * @return {@code true} if successfully advanced, otherwise {@code false}
     */
    protected boolean advance() {
        return false;
    }

    /**
     * Resets the table region.
     * @param newBasePtr the new base address
     * @param newEntryOffsetsBegin the entry offset table beginning address (inclusive)
     * @param newEntryOffsetsEnd the entry offset table ending address (exclusive)
     */
    public final void reset(long newBasePtr, long newEntryOffsetsBegin, long newEntryOffsetsEnd) {
        basePtr = newBasePtr;
        tablePtr = newEntryOffsetsBegin;
        tableEnd = newEntryOffsetsEnd;
        if (Long.compareUnsigned(tablePtr + Long.BYTES, tableEnd) <= 0) {
            long next = basePtr + UNSAFE.getLong(tablePtr);
            tablePtr += Long.BYTES;
            dataBegin = next;
            dataPtr = next;
            dataEnd = next;
        }
    }

    @Override
    public void rewind() throws IOException {
        dataPtr = dataBegin;
    }

    @Override
    public boolean hasRemaining() {
        assert Long.compareUnsigned(dataPtr, dataEnd) <= 0;
        return Long.compareUnsigned(dataPtr, dataEnd) < 0;
    }

    @Override
    public int comparePage(PageDataInput target) {
        UnsafePageDataInput other = (UnsafePageDataInput) target;
        long aPtr = dataPtr;
        long bPtr = other.dataPtr;
        long aLength = dataEnd - aPtr;
        long bLength = other.dataEnd - bPtr;
        assert aLength >= 0;
        assert bLength >= 0;
        long prefixLength = Math.min(aLength, bLength);
        int diff;
        if (prefixLength <= SMALL_PAGE_THRESHOLD) {
            // if target ranges are small, we always compares them via unsafe API
            diff = compareRegionSmall(aPtr, bPtr, (int) prefixLength);
        } else {
            diff = compareRegion(aPtr, bPtr, prefixLength);
        }
        if (diff != 0) {
            return diff;
        }
        return Long.compare(aLength, bLength);
    }

    /**
     * Compares the two region on native memory.
     * @param aPtr the first contents address
     * @param bPtr the second contents address
     * @param length the contents length
     * @return the comparison result
     */
    protected int compareRegion(long aPtr, long bPtr, long length) {
        return compareRegionLarge(aPtr, bPtr, length);
    }

    private int compareRegionLarge(long aPtr, long bPtr, long length) {
        long offset = 0;
        for (long n = length - Long.BYTES; offset <= n; offset += Long.BYTES) {
            long a = UNSAFE.getLong(aPtr + offset);
            long b = UNSAFE.getLong(bPtr + offset);
            if (a != b) {
                return compareLong(a, b);
            }
        }
        for (long n = length; offset < n; offset++) {
            byte a = UNSAFE.getByte(aPtr + offset);
            byte b = UNSAFE.getByte(bPtr + offset);
            int diff = compareByte(a, b);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static int compareRegionSmall(long aPtr, long bPtr, int length) {
        int offset = 0;
        for (int n = length - Long.BYTES; offset <= n; offset += Long.BYTES) {
            long a = UNSAFE.getLong(aPtr + offset);
            long b = UNSAFE.getLong(bPtr + offset);
            if (a != b) {
                return compareLong(a, b);
            }
        }
        for (int n = length; offset < n; offset++) {
            byte a = UNSAFE.getByte(aPtr + offset);
            byte b = UNSAFE.getByte(bPtr + offset);
            int diff = compareByte(a, b);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static int compareByte(byte a, byte b) {
        return (a & 0xff) - (b & 0xff);
    }

    private static int compareLong(long a, long b) {
        if (IS_BIG_ENDIAN) {
            return Long.compareUnsigned(a, b);
        }
        return Long.compareUnsigned(Long.reverseBytes(a), Long.reverseBytes(b));
    }

    @Override
    public final int skipBytes(int n) {
        long size = Math.min(dataEnd - dataPtr, n);
        assert size <= Integer.MAX_VALUE;
        dataPtr += size;
        return (int) size;
    }
}
