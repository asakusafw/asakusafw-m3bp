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
package com.asakusafw.m3bp.mirror.jna;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Invariants;
import com.sun.jna.Memory;

/**
 * Mock implementation of output buffer fragment.
 */
public class OutputBufferFragment {

    private final Memory contents;

    private final Memory entryOffsets;

    private final Memory keyLengths;

    private long entryCount;

    /**
     * Creates a new instance.
     * @param bufferSize the buffer size
     * @param maxEntryCount the max entry count
     * @param hasKey whether key length table is required or not
     */
    public OutputBufferFragment(long bufferSize, long maxEntryCount, boolean hasKey) {
        Arguments.require(bufferSize >= 0);
        Arguments.require(maxEntryCount >= 0);
        this.contents = new Memory(bufferSize);
        this.entryOffsets = new Memory((maxEntryCount + 1) * Long.BYTES);
        this.keyLengths = hasKey ? new Memory(maxEntryCount * Long.BYTES) : null;
    }

    /**
     * Returns whether this buffer accepts key information or not.
     * @return {@code true} this buffer accepts key information, otherwise {@code false}
     */
    public boolean hasKey() {
        return keyLengths != null;
    }

    /**
     * Returns the contents.
     * @return the contents
     */
    public Memory getContents() {
        return contents;
    }

    /**
     * Returns the entry offsets.
     * @return the entry offsets
     */
    public Memory getEntryOffsets() {
        return entryOffsets;
    }

    /**
     * Returns the key lengths.
     * @return the key lengths
     */
    public Memory getKeyLengths() {
        Invariants.requireNonNull(keyLengths);
        return keyLengths;
    }

    /**
     * Returns the entry count.
     * @return the entry count
     */
    public long getEntryCount() {
        return entryCount;
    }

    /**
     * Sets the written entry count.
     * @param entryCount the entry count
     */
    public void setEntryCount(long entryCount) {
        this.entryCount = entryCount;
    }

    /**
     * Returns the key contents.
     * @param index the target entry index
     * @return the buffer to access the target entry
     */
    public ByteBuffer getKey(long index) {
        Arguments.require(index >= 0);
        Arguments.require(index < entryCount);
        long start = entryOffsets.getLong(index * Long.BYTES);
        long length = getKeyLengths().getLong(index * Long.BYTES);
        ByteBuffer buffer = contents.getByteBuffer(start, length);
        return buffer;
    }

    /**
     * Returns the value contents.
     * @param index the target entry index
     * @return the buffer to access the target entry
     */
    public ByteBuffer getValue(long index) {
        Arguments.require(index >= 0);
        Arguments.require(index < entryCount);
        long start = entryOffsets.getLong(index * Long.BYTES);
        if (hasKey()) {
            start += getKeyLengths().getLong(index * Long.BYTES);
        }
        long end = entryOffsets.getLong((index + 1) * Long.BYTES);
        ByteBuffer buffer = contents.getByteBuffer(start, end - start);
        return buffer;
    }

    /**
     * Processes each entries.
     * @param valueConsumer consumes each value buffer
     */
    public void forEachEntries(Consumer<? super ByteBuffer> valueConsumer) {
        for (long i = 0, n = entryCount; i < n; i++) {
            valueConsumer.accept(getValue(i));
        }
    }

    /**
     * Processes each entries.
     * @param keyValueConsumer consumes each pair of key-value buffers
     */
    public void forEachEntries(BiConsumer<? super ByteBuffer, ? super ByteBuffer> keyValueConsumer) {
        for (long i = 0, n = entryCount; i < n; i++) {
            keyValueConsumer.accept(getKey(i), getValue(i));
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "OutputBufferFragment(entries={0}, contents={1}, offsets={2}, keys={3})", //$NON-NLS-1$
                entryCount,
                contents,
                entryOffsets,
                keyLengths);
    }
}
