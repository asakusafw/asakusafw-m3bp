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
package com.asakusafw.m3bp.mirror.unsafe;

import java.io.IOException;
import java.text.MessageFormat;

import com.asakusafw.m3bp.mirror.PageDataOutput;

/**
 * Unsafe implementation of {@link PageDataOutput}.
 */
@SuppressWarnings("restriction")
public class UnsafePageDataOutput extends UnsafeDataBuffer implements PageDataOutput {

    static final float MIN_LIMIT_FACTOR = .5f;

    private final float flushFactor;

    private long basePtr = 0L;

    private long dataBegin = 0L;

    private long dataThreshold = 0L;

    private long dataEnd = 0L;

    private long keyTablePtr = 0L;

    private long pageTablePtr = 0L;

    private long writtenPages = 0L;

    private long maxPages = 0L;

    /**
     * Creates a new instance.
     */
    public UnsafePageDataOutput() {
        this(0.8f);
    }

    /**
     * Creates a new instance.
     * @param flushFactor the buffer flush factor
     */
    public UnsafePageDataOutput(float flushFactor) {
        this.flushFactor = Math.min(0.99f, Math.max(MIN_LIMIT_FACTOR, flushFactor));
    }

    @Override
    public void endKey() throws IOException {
        long size = dataPtr - dataBegin;
        UNSAFE.putLong(keyTablePtr, size);
        keyTablePtr += Long.BYTES;
    }

    @Override
    public void endPage() throws IOException {
        long offset = dataPtr - basePtr;
        UNSAFE.putLong(pageTablePtr, offset);
        pageTablePtr += Long.BYTES;

        writtenPages++;
        long last = dataBegin;
        dataBegin = dataPtr;
        if (Long.compareUnsigned(writtenPages, maxPages) >= 0 || Long.compareUnsigned(dataPtr, dataThreshold) >= 0) {
            if (Long.compareUnsigned(dataPtr, dataEnd) > 0) {
                throw new IllegalStateException(MessageFormat.format(
                        "unsafe buffer overflow: buffer-size={0}, exceeded={1}, last-page-size: {2}",
                        dataEnd - basePtr,
                        dataPtr - dataEnd,
                        dataPtr - last));
            }
            flush(false);
        }
    }

    /**
     * Flushes page buffers.
     * All buffers will be disposed after this operation.
     * Clients must invoke {@link #reset(long, long, long, long, long) reset()} if continue to
     * use this object.
     * @param endOfOutput {@code true} if clients will never output after this operation
     */
    protected void flush(boolean endOfOutput) {
        return;
    }

    /**
     * Returns the written number of pages in the current session.
     * @return the written page count
     */
    protected long getWrittenCount() {
        return writtenPages;
    }

    /**
     * Resets the page buffers and starts a new session.
     * @param newContentsBegin the contents beginning address
     * @param newContentsEnd the contents ending address
     * @param newKeyLengthsBegin the key lengths buffer address (may be {@code 0})
     * @param newEntryOffsetsBegin the entry offsets buffer address
     * @param newMaxEntries the maximum numbers of entries
     */
    public final void reset(
            long newContentsBegin, long newContentsEnd,
            long newKeyLengthsBegin, long newEntryOffsetsBegin, long newMaxEntries) {
        long contentsSize = newContentsEnd - newContentsBegin;
        assert contentsSize >= 0;
        this.basePtr = newContentsBegin;
        this.dataBegin = newContentsBegin;
        this.dataPtr = newContentsBegin;
        this.dataThreshold = (long) (contentsSize * flushFactor) + newContentsBegin;
        this.dataEnd = newContentsEnd;
        this.keyTablePtr = newKeyLengthsBegin;
        this.pageTablePtr = newEntryOffsetsBegin;
        this.maxPages = newMaxEntries;
        this.writtenPages = 0L;
        if (newMaxEntries > 0) {
            UNSAFE.putLong(pageTablePtr, 0);
            pageTablePtr += Long.BYTES;
        }
    }
}
