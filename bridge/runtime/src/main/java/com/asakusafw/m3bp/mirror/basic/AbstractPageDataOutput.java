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
package com.asakusafw.m3bp.mirror.basic;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.asakusafw.lang.utils.buffer.nio.NioDataBuffer;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.PageDataOutput;

/**
 * An abstract implementation of {@link PageDataOutput}.
 */
public abstract class AbstractPageDataOutput extends NioDataBuffer implements PageDataOutput {

    static final float MIN_LIMIT_FACTOR = .5f;

    private final float flushFactor;

    private long base = 0;

    private int contentsStart = 0;

    private int entryOffsetsStart = 0;

    private ByteBuffer entryOffsets = EMPTY_BUFFER;

    private int keyLengthsStart = 0;

    private ByteBuffer keyLengths = EMPTY_BUFFER;

    private int currentLimit;

    private int currentEntryStart;

    private boolean firstPage = true;

    private int restEntries;

    /**
     * Creates a new instance.
     */
    public AbstractPageDataOutput() {
        this(0.8f);
    }

    /**
     * Creates a new instance.
     * @param flushFactor the buffer flush factor
     */
    public AbstractPageDataOutput(float flushFactor) {
        this.contents = EMPTY_BUFFER;
        this.flushFactor = Math.min(0.99f, Math.max(MIN_LIMIT_FACTOR, flushFactor));
    }

    /**
     * Returns the base offset.
     * @return the base offset
     */
    protected final long getBase() {
        return base;
    }

    /**
     * Returns the contents.
     * @return the contents
     */
    protected final ByteBuffer getContentsBuffer() {
        return contents;
    }

    /**
     * Returns the key lengths.
     * @return the key lengths
     */
    protected final ByteBuffer getKeyLengthsBuffer() {
        return keyLengths;
    }

    /**
     * Returns the entry offsets.
     * @return the entry offsets
     */
    protected final ByteBuffer getEntryOffsetsBuffer() {
        return entryOffsets;
    }

    /**
     * Resets the buffer contents.
     * @param newBase the base address of the contents buffer
     * @param newContents the contents buffer
     * @param newEntryOffsets the entry offsets buffer
     * @param newKeyLengths the key lengths buffer (optional)
     */
    public final void reset(
            long newBase,
            ByteBuffer newContents, ByteBuffer newEntryOffsets, ByteBuffer newKeyLengths) {
        Arguments.requireNonNull(newContents);
        Arguments.requireNonNull(newEntryOffsets);
        this.base = newBase;
        this.contents = newContents;
        this.contentsStart = newContents.position();
        this.entryOffsets = newEntryOffsets;
        this.entryOffsetsStart = newEntryOffsets.position();
        this.keyLengths = newKeyLengths == null ? EMPTY_BUFFER : newKeyLengths;
        this.keyLengthsStart = newKeyLengths == null ? EMPTY_BUFFER.position() : newKeyLengths.position();
        this.currentLimit = newContents.position() + (int) (newContents.remaining() * flushFactor);
        this.currentEntryStart = newContents.position();
        this.firstPage = true;
        this.restEntries = newEntryOffsets.remaining() / Long.BYTES - 1;
        if (newKeyLengths != null) {
            this.restEntries = Math.min(this.restEntries, newKeyLengths.remaining() / Long.BYTES);
        }
    }

    /**
     * Flushes page buffers.
     * All buffers will be disposed after this operation.
     * @param endOfOutput {@code true} if clients will never output after this operation
     * @throws IOException if I/O error was occurred while flushing buffers
     */
    public final void flush(boolean endOfOutput) throws IOException {
        if (entryOffsets.position() == entryOffsetsStart) {
            return;
        }
        contents.flip().position(contentsStart);
        entryOffsets.flip().position(entryOffsetsStart);
        keyLengths.flip().position(keyLengthsStart);
        doFlush(endOfOutput);
    }

    /**
     * Flushes page buffers.
     * Clients must invoke {@link #reset(long, ByteBuffer, ByteBuffer, ByteBuffer)} after this operation.
     * While flushing, each buffer will be prepared for reading their contents.
     * @param endOfOutput {@code true} if clients will never output after this operation
     * @throws IOException if I/O error was occurred while flushing buffers
     * @see #getBase()
     * @see #getContentsBuffer()
     * @see #getEntryOffsetsBuffer()
     * @see #getKeyLengthsBuffer()
     */
    protected void doFlush(boolean endOfOutput) throws IOException {
        return;
    }

    @Override
    public void endKey() throws IOException {
        int length = contents.position() - currentEntryStart;
        keyLengths.putLong(length);
    }

    @Override
    public void endPage() throws IOException {
        long current = base + currentEntryStart;
        if (firstPage) {
            entryOffsets.putLong(current);
            firstPage = false;
        }
        long next = base + contents.position();
        entryOffsets.putLong(next);
        this.currentEntryStart = contents.position();
        restEntries--;
        if (restEntries <= 0 || contents.position() > currentLimit) {
            flush(false);
        }
    }
}
