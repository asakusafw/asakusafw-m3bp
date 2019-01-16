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
package com.asakusafw.m3bp.mirror.jni;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.PageDataOutput;
import com.asakusafw.m3bp.mirror.basic.AbstractPageDataOutput;

/**
 * JNI bridge of {@link OutputWriterMirror}.
 * @since 0.1.0
 * @version 0.1.2
 */
public class OutputWriterMirrorImpl implements OutputWriterMirror, NativeMirror {

    static final Logger LOG = LoggerFactory.getLogger(OutputWriterMirrorImpl.class);

    private final Pointer reference;

    private final Output output;

    final boolean hasKey;

    private boolean ensured = false;

    private boolean closed = false;

    OutputWriterMirrorImpl(Pointer reference, float flushFactor) {
        Arguments.requireNonNull(reference);
        this.reference = reference;
        this.hasKey = hasKey0(reference.getAddress());
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

    void flush(int entryCount) {
        flush0(getPointer().getAddress(), entryCount);
    }

    void ensure() {
        long self = reference.getAddress();
        long base = getBaseOffset0(self);
        ByteBuffer contents = getContentsBuffer0(self).order(ByteOrder.nativeOrder());
        ByteBuffer entries = getEntryOffsetsBuffer0(self).order(ByteOrder.nativeOrder());
        ByteBuffer keys = hasKey ? getKeyLengthsBuffer0(self).order(ByteOrder.nativeOrder()) : null;
        output.reset(base, contents, entries, keys);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        if (ensured && closed == false) {
            output.flush(true);
            close0(getPointer().getAddress());
            ensured = false;
            closed = true;
            if (LOG.isTraceEnabled()) {
                LOG.trace("total written: size={}, entries={}, ave-size={}", //$NON-NLS-1$
                        output.totalSize,
                        output.totalEntries,
                        (double) output.totalSize / output.totalEntries);
            }
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "OutputWriterMirror[{0}]", //$NON-NLS-1$
                getPointer());
    }

    private static native boolean hasKey0(long self);

    private static native long getBaseOffset0(long self);

    private static native ByteBuffer getContentsBuffer0(long self);

    private static native ByteBuffer getEntryOffsetsBuffer0(long self);

    private static native ByteBuffer getKeyLengthsBuffer0(long self);

    private static native void flush0(long self, int recordCount);

    private static native void close0(long self);

    private class Output extends AbstractPageDataOutput {

        long totalEntries = 0;

        long totalSize = 0;

        Output(float flushFactor) {
            super(flushFactor);
        }

        @Override
        protected void doFlush(boolean endOfOutput) throws IOException {
            int entries = getEntryOffsetsBuffer().remaining() / Long.BYTES - 1;
            if (LOG.isTraceEnabled()) {
                long contentsSize = getContentsBuffer().remaining();
                long offsetsSize = getEntryOffsetsBuffer().remaining();
                long keysSize = getKeyLengthsBuffer().remaining();
                LOG.trace("flushing output: contents={}, entries={}, keys={}", //$NON-NLS-1$
                        contentsSize,
                        entries,
                        keysSize / Long.BYTES);
                totalEntries += entries;
                totalSize += contentsSize + offsetsSize + keysSize;
                verifyInvariants(entries);
            }
            OutputWriterMirrorImpl.this.flush(entries);
            if (endOfOutput == false) {
                OutputWriterMirrorImpl.this.ensure();
            }
        }

        private void verifyInvariants(int entryCount) {
            assert entryCount > 0;
            long base = getBase();
            ByteBuffer buf = getContentsBuffer();
            long begin = base + buf.position();
            long end = base + buf.limit();
            ByteBuffer entries = getEntryOffsetsBuffer();
            if (hasKey) {
                ByteBuffer keys = getKeyLengthsBuffer();
                int entryOffset = entries.position();
                int keyOffset = keys.position();
                assert keys.remaining() / Long.BYTES == entryCount;
                for (int index = 0; index < entryCount; index++) {
                    long offset = entries.getLong(entryOffset + index * Long.BYTES);
                    long limit = entries.getLong(entryOffset + (index + 1) * Long.BYTES);
                    long key = keys.getLong(keyOffset + index * Long.BYTES);
                    long keyLimit = offset + key;
                    assert begin <= offset;
                    assert offset <= keyLimit;
                    assert keyLimit < limit; // page contents must not be empty
                    assert limit <= end;
                }
            } else {
                int entryOffset = entries.position();
                for (int index = 0; index < entryCount; index++) {
                    long offset = entries.getLong(entryOffset + index * Long.BYTES);
                    long limit = entries.getLong(entryOffset + (index + 1) * Long.BYTES);
                    assert begin <= offset;
                    assert offset < limit; // page contents must not be empty
                    assert limit <= end;
                }
            }
        }
    }
}
