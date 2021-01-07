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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Invariants;
import com.sun.jna.Memory;

/**
 * Utilities for input/output buffers.
 */
public final class BufferUtil {

    private BufferUtil() {
        return;
    }

    /**
     * Creates a new input buffer fragment.
     * @param size the buffer size
     * @param entries the contents writer
     * @return the created fragment
     */
    @SafeVarargs
    public static InputBufferFragment input(long size, Consumer<ByteBuffer>... entries) {
        OutputBufferFragment output = output(size, entries);
        return new InputBufferFragment(output.getContents(), output.getEntryOffsets(), output.getEntryCount());
    }

    /**
     * Creates a new output buffer fragment.
     * @param size the buffer size
     * @param entries the contents writer
     * @return the created fragment
     */
    @SafeVarargs
    public static OutputBufferFragment output(long size, Consumer<ByteBuffer>... entries) {
        OutputBufferFragment fragment = new OutputBufferFragment(size, entries.length, false);
        ByteBuffer buf = fragment.getContents().getByteBuffer(0, size);
        Memory offsets = fragment.getEntryOffsets();
        if (entries.length > 0) {
            offsets.setLong(0, 0);
            for (int i = 0; i < entries.length; i++) {
                entries[i].accept(buf);
                offsets.setLong((i + 1) * Long.BYTES, buf.position());
            }
        }
        fragment.setEntryCount(entries.length);
        return fragment;
    }

    /**
     * Creates a new output buffer fragment.
     * @param size the buffer size
     * @param pairs key-value pairs writer
     * @return the created fragment
     */
    @SafeVarargs
    public static OutputBufferFragment outputWithKeys(long size, Consumer<ByteBuffer>... pairs) {
        Arguments.require(pairs.length % 2 == 0);
        int entryCount = pairs.length / 2;
        OutputBufferFragment fragment = new OutputBufferFragment(size, entryCount, true);
        ByteBuffer buf = fragment.getContents().getByteBuffer(0, size);
        Memory keys = fragment.getKeyLengths();
        Memory offsets = fragment.getEntryOffsets();

        offsets.setLong(0, 0);
        for (int i = 0; i < entryCount; i++) {
            int starting = buf.position();
            pairs[i * 2 + 0].accept(buf);
            int keyEnd = buf.position();
            pairs[i * 2 + 1].accept(buf);
            int valueEnd = buf.position();
            keys.setLong(i * Long.BYTES, keyEnd - starting);
            offsets.setLong((i + 1) * Long.BYTES, valueEnd);
        }
        fragment.setEntryCount(entryCount);
        return fragment;
    }

    /**
     * Collects contents in the {@link InputBufferCursor}.
     * @param input the input (must be value-only)
     * @param consumer the target consumer
     */
    public static void collect(InputBufferCursor input, Consumer<ByteBuffer> consumer) {
        Arguments.requireNonNull(input);
        Arguments.requireNonNull(consumer);
        Arguments.require(input.hasKey() == false);
        while (true) {
            InputBufferFragment value = input.nextValue();
            if (value == null) {
                break;
            }
            for (long i = 0, n = value.getEntryCount(); i < n; i++) {
                ByteBuffer buf = value.getEntry(i);
                consumer.accept(buf);
            }
        }
    }

    /**
     * Collects contents in the {@link InputBufferCursor}.
     * @param input the input (must be key-value)
     * @param consumer the target key-value consumer
     */
    public static void collect(InputBufferCursor input, BiConsumer<ByteBuffer, ByteBuffer> consumer) {
        Arguments.requireNonNull(input);
        Arguments.requireNonNull(consumer);
        Arguments.require(input.hasKey());
        while (true) {
            InputBufferFragment key = input.nextKey();
            InputBufferFragment value = input.nextValue();
            if (key == null) {
                Invariants.require(value == null);
                break;
            }
            Invariants.requireNonNull(value);
            Invariants.require(key.getEntryCount() == value.getEntryCount());
            for (long i = 0, n = key.getEntryCount(); i < n; i++) {
                ByteBuffer keyBuf = key.getEntry(i);
                ByteBuffer valueBuf = value.getEntry(i);
                consumer.accept(keyBuf, valueBuf);
            }
        }
    }
}
