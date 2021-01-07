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
package com.asakusafw.m3bp.compiler.comparator;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Adapter for {@link ByteBuffer} into {@link DataOutput}.
 */
public class ByteBufferDataOutput implements DataOutput {

    private final ByteBuffer buffer;

    /**
     * Creates a new instance.
     * @param buffer the target buffer
     */
    public ByteBufferDataOutput(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Returns the buffer.
     * @return the buffer
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public final void write(int b) throws IOException {
        buffer.put((byte) b);
    }

    @Override
    public final void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public final void write(byte[] b, int off, int len) throws IOException {
        buffer.put(b, off, len);
    }

    @Override
    public final void writeBoolean(boolean v) throws IOException {
        buffer.put(v ? (byte) 1 : (byte) 0);
    }

    @Override
    public final void writeByte(int v) throws IOException {
        buffer.put((byte) v);
    }

    @Override
    public final void writeShort(int v) throws IOException {
        buffer.putShort((short) v);
    }

    @Override
    public final void writeChar(int v) throws IOException {
        buffer.putChar((char) v);

    }

    @Override
    public final void writeInt(int v) throws IOException {
        buffer.putInt(v);
    }

    @Override
    public final void writeLong(long v) throws IOException {
        buffer.putLong(v);
    }

    @Override
    public final void writeFloat(float v) throws IOException {
        buffer.putFloat(v);
    }

    @Override
    public final void writeDouble(double v) throws IOException {
        buffer.putDouble(v);
    }

    @Override
    public final void writeBytes(String s) throws IOException {
        for (int i = 0, n = s.length(); i < n; i++) {
            writeByte(s.charAt(i));
        }
    }

    @Override
    public final void writeChars(String s) throws IOException {
        for (int i = 0, n = s.length(); i < n; i++) {
            writeChar(s.charAt(i));
        }
    }

    @Override
    public final void writeUTF(String s) throws IOException {
        throw new UnsupportedOperationException();
    }
}
