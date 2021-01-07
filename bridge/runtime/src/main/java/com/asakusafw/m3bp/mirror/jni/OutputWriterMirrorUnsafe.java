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
package com.asakusafw.m3bp.mirror.jni;

import java.io.IOException;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.PageDataOutput;
import com.asakusafw.m3bp.mirror.unsafe.UnsafePageDataOutput;

/**
 * Unsafe implementation of {@link OutputWriterMirror}.
 * @since 0.1.0
 * @version 0.1.2
 */
public final class OutputWriterMirrorUnsafe implements OutputWriterMirror, NativeMirror {

    private static final int VALUES_SIZE = 5;

    static final Logger LOG = LoggerFactory.getLogger(OutputWriterMirrorUnsafe.class);

    private static final int INDEX_BUFFER_PTR = 0;

    private static final int INDEX_BUFFER_SIZE = 1;

    private static final int INDEX_OFFSET_TABLE_PTR = 2;

    private static final int INDEX_KEY_LENGTH_TABLE_PTR = 3;

    private static final int INDEX_RECORD_COUNT = 4;

    private final long[] values = new long[VALUES_SIZE];

    private final Pointer reference;

    private final Output output;

    private boolean ensured = false;

    private boolean closed = false;

    OutputWriterMirrorUnsafe(Pointer reference, float flushFactor) {
        Arguments.requireNonNull(reference);
        this.reference = reference;
        this.output = new Output(flushFactor);
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    public PageDataOutput getOutput() {
        if (ensured == false) {
            ensured = true;
            ensure();
        }
        return output;
    }

    void ensure() {
        allocateBuffer0(reference.getAddress(), values);
        long bufferPtr = values[INDEX_BUFFER_PTR];
        long bufferSize = values[INDEX_BUFFER_SIZE];
        long offsetTablePtr = values[INDEX_OFFSET_TABLE_PTR];
        long keyLengthTablePtr = values[INDEX_KEY_LENGTH_TABLE_PTR];
        long recordCount = values[INDEX_RECORD_COUNT];
        long bufferEnd = bufferPtr + bufferSize;
        output.reset(bufferPtr, bufferEnd,
                keyLengthTablePtr, offsetTablePtr,
                recordCount);
    }

    void flush(long count) {
        flush0(reference.getAddress(), count);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        if (ensured && closed == false) {
            output.flush(true);
            close0(getPointer().getAddress());
            ensured = false;
            closed = true;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "OutputWriterMirror[{0}]", //$NON-NLS-1$
                getPointer());
    }

    private static native void allocateBuffer0(long self, long[] values);

    private static native void flush0(long self, long recordCount);

    private static native void close0(long self);

    private class Output extends UnsafePageDataOutput {

        Output(float flushFactor) {
            super(flushFactor);
        }

        @Override
        public void flush(boolean endOfOutput) {
            long count = getWrittenCount();
            if (count != 0L) {
                OutputWriterMirrorUnsafe.this.flush(count);
            }
            if (endOfOutput) {
                reset(0L, 0L, 0L, 0L, 0L);
            } else {
                OutputWriterMirrorUnsafe.this.ensure();
            }
        }
    }
}
