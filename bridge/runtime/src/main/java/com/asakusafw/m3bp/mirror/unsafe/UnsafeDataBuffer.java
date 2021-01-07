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
package com.asakusafw.m3bp.mirror.unsafe;

import java.io.IOException;

import com.asakusafw.lang.utils.buffer.DataBuffer;
import com.asakusafw.lang.utils.buffer.DataIoUtils;
import com.asakusafw.lang.utils.common.Invariants;

/**
 * Unsafe implementation of {@link DataBuffer}.
 * @since 0.4.0
 */
@SuppressWarnings("restriction")
public class UnsafeDataBuffer implements DataBuffer {

    /**
     * The Unsafe API.
     */
    protected static final sun.misc.Unsafe UNSAFE;
    static {
        Invariants.require(UnsafeUtil.isAvailable());
        UNSAFE = (sun.misc.Unsafe) UnsafeUtil.API;
        if (UNSAFE == null) {
            throw new AssertionError();
        }
    }

    /**
     * The contents offset from {@code byte[]} object reference.
     */
    protected static final int OFFSET_BYTE_ARRAY = sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

    /**
     * The next contents address.
     */
    public long dataPtr = 0L;

    @Override
    public final boolean readBoolean() {
        byte result = UNSAFE.getByte(dataPtr);
        dataPtr += Byte.BYTES;
        return result != 0;
    }

    @Override
    public final byte readByte() {
        byte result = UNSAFE.getByte(dataPtr);
        dataPtr += Byte.BYTES;
        return result;
    }

    @Override
    public final short readShort() {
        short result = UNSAFE.getShort(dataPtr);
        dataPtr += Short.BYTES;
        return result;
    }

    @Override
    public final char readChar() {
        char result = UNSAFE.getChar(dataPtr);
        dataPtr += Character.BYTES;
        return result;
    }

    @Override
    public final int readInt() {
        int result = UNSAFE.getInt(dataPtr);
        dataPtr += Integer.BYTES;
        return result;
    }

    @Override
    public final long readLong() {
        long result = UNSAFE.getLong(dataPtr);
        dataPtr += Long.BYTES;
        return result;
    }

    @Override
    public final float readFloat() {
        float result = UNSAFE.getFloat(dataPtr);
        dataPtr += Float.BYTES;
        return result;
    }

    @Override
    public final double readDouble() {
        double result = UNSAFE.getDouble(dataPtr);
        dataPtr += Double.BYTES;
        return result;
    }

    @Override
    public final void readFully(byte[] b) {
        readFully(b, 0, b.length);
    }

    @Override
    public final void readFully(byte[] b, int off, int len) {
        UNSAFE.copyMemory(
                null, dataPtr,
                b, OFFSET_BYTE_ARRAY + off,
                len);
        dataPtr += len;
    }

    @Override
    public int skipBytes(int n) {
        dataPtr += n;
        return n;
    }

    @Override
    public final int readUnsignedByte() {
        return readByte() & 0xff;
    }

    @Override
    public final int readUnsignedShort() {
        return readShort() & 0xffff;
    }

    @Override
    public final String readLine() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final String readUTF() {
        try {
            return DataIoUtils.readUTF(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public final void write(int b) {
        UNSAFE.putByte(dataPtr, (byte) b);
        dataPtr += Byte.BYTES;
    }

    @Override
    public final void writeBoolean(boolean v) {
        UNSAFE.putByte(dataPtr, v ? (byte) 1 : (byte) 0);
        dataPtr += Byte.BYTES;
    }

    @Override
    public final void writeByte(int v) {
        UNSAFE.putByte(dataPtr, (byte) v);
        dataPtr += Byte.BYTES;
    }

    @Override
    public final void writeShort(int v) {
        UNSAFE.putShort(dataPtr, (short) v);
        dataPtr += Short.BYTES;
    }

    @Override
    public final void writeChar(int v) {
        UNSAFE.putChar(dataPtr, (char) v);
        dataPtr += Character.BYTES;
    }

    @Override
    public final void writeInt(int v) {
        UNSAFE.putInt(dataPtr, v);
        dataPtr += Integer.BYTES;
    }

    @Override
    public final void writeLong(long v) {
        UNSAFE.putLong(dataPtr, v);
        dataPtr += Long.BYTES;
    }

    @Override
    public final void writeFloat(float v) {
        UNSAFE.putFloat(dataPtr, v);
        dataPtr += Float.BYTES;
    }

    @Override
    public final void writeDouble(double v) {
        UNSAFE.putDouble(dataPtr, v);
        dataPtr += Double.BYTES;
    }

    @Override
    public final void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public final void write(byte[] b, int off, int len) {
        UNSAFE.copyMemory(
                b, OFFSET_BYTE_ARRAY + off,
                null, dataPtr,
                len);
        dataPtr += len;
    }

    @Override
    public final void writeBytes(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            UNSAFE.putByte(dataPtr + i * Byte.BYTES, (byte) s.charAt(i));
        }
        dataPtr += s.length() * Byte.BYTES;
    }

    @Override
    public final void writeChars(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            UNSAFE.putChar(dataPtr + i * Character.BYTES, s.charAt(i));
        }
        dataPtr += s.length() * Character.BYTES;
    }

    @Override
    public final void writeUTF(String s) {
        try {
            DataIoUtils.writeUTF(this, s);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
