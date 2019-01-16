/**
 * Copyright 2011-2019 Asakusa Framework Team.
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
import java.util.function.Consumer;

import com.asakusafw.lang.utils.common.Arguments;
import com.sun.jna.Memory;

/**
 * Mock implementation of input buffer fragment.
 */
public class InputBufferFragment {

    private final Memory contents;

    private final Memory entryOffsets;

    private final long entryCount;

    /**
     * Creates a new instance.
     * @param contents the contents buffer
     * @param entryOffsets the entry buffer
     * @param entryCount the entry count
     */
    public InputBufferFragment(Memory contents, Memory entryOffsets, long entryCount) {
        Arguments.requireNonNull(contents);
        Arguments.requireNonNull(entryOffsets);
        this.contents = contents;
        this.entryOffsets = entryOffsets;
        this.entryCount = entryCount;
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
     * Returns the entry count.
     * @return the entry count
     */
    public long getEntryCount() {
        return entryCount;
    }

    /**
     * Returns the entry contents.
     * @param index the target entry index
     * @return the buffer to access the target entry
     */
    public ByteBuffer getEntry(long index) {
        Arguments.require(index >= 0);
        Arguments.require(index < entryCount);
        long start = entryOffsets.getLong(index * Long.BYTES);
        long end = entryOffsets.getLong((index + 1) * Long.BYTES);
        ByteBuffer buffer = contents.getByteBuffer(start, end - start);
        return buffer;
    }

    /**
     * Processes each entries.
     * @param entryConsumer consumes each entry buffer
     */
    public void forEachEntries(Consumer<? super ByteBuffer> entryConsumer) {
        for (long i = 0, n = entryCount; i < n; i++) {
            entryConsumer.accept(getEntry(i));
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "InputBufferFragment(entries={0}, contents={1}, offsets={2})", //$NON-NLS-1$
                entryCount,
                contents,
                entryOffsets);
    }
}
