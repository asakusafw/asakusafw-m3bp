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

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.asakusafw.lang.utils.buffer.DataIoUtils;
import com.asakusafw.lang.utils.common.Arguments;

class NativeDataInput implements DataInput {

    static final int BUFFER_CHUNK_SIZE = 1 << 30;

    static final int BUFFER_CHUNK_LIMIT = BUFFER_CHUNK_SIZE + (BUFFER_CHUNK_SIZE / 4);

    private final long dataPtr;

    private final long dataLength;

    private long dataLimit;

    private long chunkOffset;

    private ByteBuffer chunkView;

    private boolean canFeed;

    NativeDataInput(long dataPtr, long length) {
        Arguments.require(length >= 0);
        Arguments.require(Long.compareUnsigned(dataPtr + length, dataPtr) >= 0);
        this.dataPtr = dataPtr;
        this.dataLength = length;
        this.dataLimit = dataLength;
        this.chunkOffset = 0L;
        this.canFeed = true;
        setPosition0(0L);
    }

    /**
     * Returns the base offset.
     * @return the base offset
     */
    public final long getBase() {
        return dataPtr;
    }

    /**
     * Returns the current reading position.
     * @return the current reading position (relative from {@link #getBase() base address})
     */
    public final long position() {
        return chunkOffset + chunkView.position();
    }

    /**
     * Sets the reading position.
     * @param position the reading position (relative from {@link #getBase() base address})
     */
    public final void position(long position) {
        long offset = position - chunkOffset;
        ByteBuffer buf = chunkView;
        if (0 <= offset && offset <= buf.capacity()) {
            buf.position((int) offset);
        } else {
            setPosition0(position);
        }
    }

    /**
     * Returns the read limit.
     * @return the read limit (relative from {@link #getBase() base address})
     */
    public final long limit() {
        return this.dataLimit;
    }

    /**
     * Sets the read limit.
     * @param limit the limit position (relative from {@link #getBase() base address})
     */
    public final void limit(long limit) {
        this.dataLimit = limit;
        refreshLimit();
    }

    /**
     * Sets the reading region.
     * @param position the reading position
     * @param length the available length in bytes
     */
    public final void region(long position, long length) {
        this.dataLimit = position + length;
        refreshLimit();
        position(position);
    }

    /**
     * Returns whether or not there are remaining data in this input.
     * @return {@code true} if this input has remaining bytes
     */
    public final boolean hasRemaining() {
        return chunkView.hasRemaining() || remaining() > 0;
    }

    /**
     * Returns the read remaining bytes.
     * @return the read remaining bytes
     */
    public final long remaining() {
        return limit() - position();
    }

    /**
     * Returns the total data length.
     * @return the total data length
     */
    public final long capacity() {
        return dataLength;
    }

    @Override
    public final boolean readBoolean() {
        feed();
        return chunkView.get() != 0;
    }

    @Override
    public final byte readByte() {
        feed();
        return chunkView.get();
    }

    @Override
    public final int readUnsignedByte() {
        feed();
        return chunkView.get() & 0xff;
    }

    @Override
    public final short readShort() {
        feed();
        return chunkView.getShort();
    }

    @Override
    public final int readUnsignedShort() {
        feed();
        return chunkView.getShort() & 0xffff;
    }

    @Override
    public final char readChar() {
        feed();
        return chunkView.getChar();
    }

    @Override
    public final int readInt() {
        feed();
        return chunkView.getInt();
    }

    @Override
    public final long readLong() {
        feed();
        return chunkView.getLong();
    }

    @Override
    public final float readFloat() {
        feed();
        return chunkView.getFloat();
    }

    @Override
    public final double readDouble() {
        feed();
        return chunkView.getDouble();
    }

    @Override
    public final void readFully(byte[] b) {
        readFully(b, 0, b.length);
    }

    @Override
    public final void readFully(byte[] b, int off, int len) {
        feed();
        chunkView.get(b, off, len);
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
    public final int skipBytes(int n) {
        if (n <= 0) {
            return 0;
        }
        feed();
        int skip = Math.min(n, chunkView.remaining());
        if (skip > 0) {
            chunkView.position(chunkView.position() + skip);
        }
        return skip;
    }

    /**
     * Compares each data region as unsigned byte sequences.
     * @param a the first data input
     * @param b the second data input
     * @return the comparing result
     */
    public static int compareRegion(NativeDataInput a, NativeDataInput b) {
        long aPtr = a.getBase() + a.position();
        long bPtr = b.getBase() + b.position();
        long aLength = a.remaining();
        long bLength = b.remaining();
        long prefixLength = Math.min(aLength, bLength);
        int diff = NativeBufferUtil.compare(aPtr, bPtr, prefixLength);
        if (diff != 0) {
            return diff;
        }
        return Long.compare(aLength, bLength);
    }

    @Override
    public String toString() {
        return String.format(
                "NativeData(data=%016xh+%,d, region=%,d+%,d)",
                getBase(),
                capacity(),
                position(),
                remaining());
    }

    private void feed() {
        if (canFeed && chunkView.position() >= BUFFER_CHUNK_LIMIT) {
            setPosition0(chunkOffset + chunkView.position());
        }
    }

    private void setPosition0(long position) {
        assert position >= 0;
        int localPosition = (int) (position % BUFFER_CHUNK_SIZE);
        long nextChunkOffset = position - localPosition;
        int nextChunkLength = (int) Math.min(Integer.MAX_VALUE, dataLength - nextChunkOffset);
        assert localPosition <= nextChunkLength : localPosition;

        ByteBuffer nextChunk = NativeBufferUtil.getView(dataPtr + nextChunkOffset, nextChunkLength);
        nextChunk.limit(nextChunk.capacity());
        nextChunk.position(localPosition);
        chunkOffset = nextChunkOffset;
        chunkView = nextChunk;
        canFeed = nextChunkOffset + nextChunkLength < dataLength;
        refreshLimit();
    }

    private void refreshLimit() {
        ByteBuffer buf = chunkView;
        buf.limit((int) Math.max(buf.position(), Math.min(dataLimit - chunkOffset, buf.capacity())));
    }
}
