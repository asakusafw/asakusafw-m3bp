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

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Invariants;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.PageDataInput;
import com.asakusafw.m3bp.mirror.basic.AbstractPageDataInput;
import com.sun.jna.Memory;

/**
 * {@link InputReaderMirror} using {@link InputBufferFragment}.
 */
public class BufferInputReaderMirror implements InputReaderMirror {

    private final Input key;

    private final Input value;

    /**
     * Creates a new instance.
     * @param cursor the input
     */
    public BufferInputReaderMirror(InputBufferCursor cursor) {
        Arguments.requireNonNull(cursor);
        this.key = cursor.hasKey() ? new Input(cursor::nextKey) : null;
        this.value = new Input(cursor::nextValue);
    }

    @Override
    public PageDataInput getKeyInput() {
        Invariants.requireNonNull(key);
        return key;
    }

    @Override
    public PageDataInput getValueInput() {
        return value;
    }

    private static class Input extends AbstractPageDataInput {

        private static final ByteBuffer EMPTY = ByteBuffer.allocateDirect(0);

        private final Supplier<? extends InputBufferFragment> fragments;

        private InputBufferFragment current;

        private long currentRecord;

        Input(Supplier<? extends InputBufferFragment> fragments) {
            this.fragments = fragments;
            reset(0, EMPTY, EMPTY);
        }

        @Override
        protected boolean doAdvance() {
            while (current == null || currentRecord >= current.getEntryCount()) {
                current = fragments.get();
                currentRecord = 0;
                if (current == null) {
                    return false;
                }
            }
            doReset();
            return true;
        }

        private static final long MAX_RECORDS = Integer.MAX_VALUE / Long.BYTES - 1;

        private static final long MAX_BUFFER_SIZE = Integer.MAX_VALUE;

        private void doReset() {
            assert current != null;
            assert currentRecord < current.getEntryCount();

            Memory data = current.getContents();
            Memory offsets = current.getEntryOffsets();

            long restRecords = Math.min(current.getEntryCount() - currentRecord, MAX_RECORDS);
            long nextRecord = currentRecord + restRecords;
            long contentsBase = offsets.getLong(currentRecord * Long.BYTES);
            long contentsLimit = offsets.getLong(nextRecord * Long.BYTES);
            assert contentsLimit - contentsBase <= MAX_BUFFER_SIZE;

            ByteBuffer contentsBuf = data.getByteBuffer(contentsBase, contentsLimit - contentsBase);
            ByteBuffer offsetsBuf = offsets.getByteBuffer(currentRecord * Long.BYTES, (nextRecord + 1) * Long.BYTES);
            currentRecord = nextRecord;

            reset(contentsBase, contentsBuf, offsetsBuf);
        }
    }
}
