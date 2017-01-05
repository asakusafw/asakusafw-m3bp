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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.PageDataInput;
import com.asakusafw.m3bp.mirror.basic.AbstractPageDataInput;

/**
 * JNI bridge of {@link InputReaderMirror}.
 */
public class InputReaderMirrorImpl implements InputReaderMirror, NativeMirror {

    final Pointer reference;

    private final Input key;

    private final Input value;

    private boolean closed = false;

    InputReaderMirrorImpl(Pointer reference) {
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
        Input input = new Input(isKey);
        reset(input, isKey);
        return input;
    }

    boolean advance(Input input, boolean isKey) {
        boolean advanced = advance0(getPointer().getAddress(), isKey);
        if (advanced == false) {
            return false;
        }
        reset(input, isKey);
        return true;
    }

    static int compareBuffers(ByteBuffer a, ByteBuffer b) {
        return compareBuffers0(
                a, a.position(), a.remaining(),
                b, b.position(), b.remaining());
    }

    private void reset(Input input, boolean isKey) {
        long self = getPointer().getAddress();
        long base = getBaseOffset0(self, isKey);
        ByteBuffer contents = getContentsBuffer0(self, isKey).order(ByteOrder.nativeOrder());
        ByteBuffer offsets = getEntryOffsetsBuffer0(self, isKey).order(ByteOrder.nativeOrder());
        assert contents != null;
        assert offsets != null;
        input.reset(base, contents, offsets);
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "InputReaderMirror[{0}]", //$NON-NLS-1$
                getPointer());
    }

    private static native boolean hasKey0(long self);

    private static native long getBaseOffset0(long self, boolean isKey);

    private static native int compareBuffers0(
            ByteBuffer buf0, int off0, int len0,
            ByteBuffer buf1, int off1, int len1);

    private static native ByteBuffer getContentsBuffer0(long self, boolean isKey);

    private static native ByteBuffer getEntryOffsetsBuffer0(long self, boolean isKey);

    private static native boolean advance0(long address, boolean isKey);

    private static native void close0(long address);

    private class Input extends AbstractPageDataInput {

        private final boolean keyBuffer;

        Input(boolean keyBuffer) {
            this.keyBuffer = keyBuffer;
        }

        @Override
        protected boolean doAdvance() {
            return InputReaderMirrorImpl.this.advance(this, keyBuffer);
        }

        @Override
        public int comparePage(PageDataInput target) {
            ByteBuffer a = getContentsBuffer();
            ByteBuffer b = ((Input) target).getContentsBuffer();
            return compareBuffers(a, b);
        }
    }
}
