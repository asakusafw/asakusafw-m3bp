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

import java.io.IOException;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.PageDataInput;
import com.asakusafw.m3bp.mirror.unsafe.UnsafePageDataInput;

/**
 * Unsafe implementation of {@link InputReaderMirror}.
 */
public class InputReaderMirrorUnsafe implements InputReaderMirror, NativeMirror {

    static final Logger LOG = LoggerFactory.getLogger(InputReaderMirrorUnsafe.class);

    private static final int VALUES_SIZE = 3;

    private static final int INDEX_BUFFER_PTR = 0;

    private static final int INDEX_OFFSET_TABLE_PTR = 1;

    private static final int INDEX_RECORD_COUNT = 2;

    private final long[] values = new long[VALUES_SIZE];

    final Pointer reference;

    private final Input key;

    private final Input value;

    private boolean closed = false;

    InputReaderMirrorUnsafe(Pointer reference) {
        Arguments.requireNonNull(reference);
        boolean hasKey = hasKey0(reference.getAddress());
        this.reference = reference;
        this.key = hasKey ? initialize(true) : null;

        // Note: without key, values are in key_buffer (not in value_buffer)
        this.value = hasKey ? initialize(false) : initialize(true);
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    public PageDataInput getKeyInput() {
        if (key == null) {
            throw new UnsupportedOperationException();
        }
        return key;
    }

    @Override
    public PageDataInput getValueInput() {
        return value;
    }

    @Override
    public void close() throws IOException, InterruptedException {
        if (closed == false) {
            close0(getPointer().getAddress());
            closed = true;
        }
    }

    private Input initialize(boolean isKey) {
        getInputBufferFragment0(reference.getAddress(), isKey, values);
        long bufferPtr = values[INDEX_BUFFER_PTR];
        long offsetTableBegin = values[INDEX_OFFSET_TABLE_PTR];
        long recordCount = values[INDEX_RECORD_COUNT];
        long offsetTableEnd = offsetTableBegin + (recordCount == 0 ? 0 : (recordCount + 1) * Long.BYTES);
        return new Input(bufferPtr, offsetTableBegin, offsetTableEnd);
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "InputReaderMirror[{0}]", //$NON-NLS-1$
                getPointer());
    }

    private static native boolean hasKey0(long self);

    private static native void getInputBufferFragment0(long address, boolean isKey, long[] target);

    private static native void close0(long address);

    private static class Input extends UnsafePageDataInput {

        Input(long dataPtr, long entryOffsetsPtr, long entryOffsetsEnd) {
            reset(dataPtr, entryOffsetsPtr, entryOffsetsEnd);
        }

        @Override
        protected int compareRegion(long aPtr, long bPtr, long length) {
            return NativeBufferUtil.compare(aPtr, bPtr, length);
        }
    }
}
