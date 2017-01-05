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
package com.asakusafw.m3bp.mirror.basic;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.asakusafw.lang.utils.buffer.nio.NioDataBuffer;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.PageDataInput;

/**
 * An abstract implementation of {@link PageDataInput}.
 */
public abstract class AbstractPageDataInput extends NioDataBuffer implements PageDataInput {

    private long base = 0;

    private ByteBuffer entryOffsets = EMPTY_BUFFER;

    private int rewindPosition;

    private int restEntries;

    /**
     * Creates a new instance.
     */
    public AbstractPageDataInput() {
        this.contents = EMPTY_BUFFER;
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
     */
    public final void reset(long newBase, ByteBuffer newContents, ByteBuffer newEntryOffsets) {
        Arguments.requireNonNull(newContents);
        Arguments.requireNonNull(newEntryOffsets);
        this.base = newBase;
        this.contents = newContents;
        this.entryOffsets = newEntryOffsets;
        this.restEntries = newEntryOffsets.remaining() / Long.BYTES - 1;
        if (restEntries > 0) {
            int first = (int) (newEntryOffsets.getLong() - base);
            newContents.position(0).limit(first);
            this.rewindPosition = 0;
        } else {
            newContents.limit(newContents.position());
            this.rewindPosition = newContents.limit();
        }
    }

    private boolean advance() {
        return doAdvance();
    }

    /**
     * Obtains next available page data chunk.
     * Clients should override this method.
     * @return {@code true} if the next chunk exists, otherwise {@code false}
     */
    protected boolean doAdvance() {
        return false;
    }

    @Override
    public final boolean next() throws IOException {
        while (restEntries <= 0) {
            if (advance() == false) {
                return false;
            }
        }
        int position = contents.limit();
        int limit = (int) (entryOffsets.getLong() - base);
        contents.limit(limit).position(position);
        rewindPosition = position;
        restEntries--;
        return true;
    }

    @Override
    public boolean hasRemaining() {
        return contents.hasRemaining();
    }

    @Override
    public void rewind() throws IOException {
        contents.position(rewindPosition);
    }

    @Override
    public int comparePage(PageDataInput target) {
        return contents.compareTo(((AbstractPageDataInput) target).contents);
    }
}
