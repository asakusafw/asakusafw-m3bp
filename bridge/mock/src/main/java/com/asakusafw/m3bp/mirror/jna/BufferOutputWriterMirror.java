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
package com.asakusafw.m3bp.mirror.jna;

import static com.asakusafw.m3bp.mirror.jna.Util.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.PageDataOutput;
import com.asakusafw.m3bp.mirror.basic.AbstractPageDataOutput;

/**
 * {@link OutputWriterMirror} using {@link OutputBufferFragment}.
 */
public class BufferOutputWriterMirror implements OutputWriterMirror {

    static final Logger LOG = LoggerFactory.getLogger(BufferOutputWriterMirror.class);

    private final Supplier<? extends OutputBufferFragment> allocator;

    private final Consumer<? super OutputBufferFragment> sink;

    private OutputBufferFragment current;

    private final Output output = new Output();

    private boolean closed = false;

    /**
     * Creates a new instance.
     * @param allocator the buffer allocator
     * @param sink the written buffer sink
     */
    public BufferOutputWriterMirror(
            Supplier<? extends OutputBufferFragment> allocator,
            Consumer<? super OutputBufferFragment> sink) {
        Arguments.requireNonNull(allocator);
        Arguments.requireNonNull(sink);
        this.allocator = allocator;
        this.sink = sink;
        ensure();
    }

    @Override
    public PageDataOutput getOutput() {
        return output;
    }

    void flush(int entryCount) {
        assert current != null;
        current.setEntryCount(entryCount);
        sink.accept(current);
        current = null;
    }

    void ensure() {
        assert current == null;
        this.current = allocator.get();
        ByteBuffer contents = toBuffer(current.getContents());
        ByteBuffer entries = toBuffer(current.getEntryOffsets());
        ByteBuffer keys = current.hasKey() ? toBuffer(current.getKeyLengths()) : null;
        output.reset(0, contents, entries, keys);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        if (closed == false) {
            output.flush(true);
            closed = true;
        }
    }

    private class Output extends AbstractPageDataOutput {

        Output() {
            return;
        }

        @Override
        protected void doFlush(boolean endOfOutput) throws IOException {
            int entries = getEntryOffsetsBuffer().remaining() / Long.BYTES - 1;
            if (LOG.isTraceEnabled()) {
                LOG.trace("flushing output: contents={}, entries={}, keys={}", //$NON-NLS-1$
                        getContentsBuffer().remaining(),
                        entries,
                        getKeyLengthsBuffer().remaining() / Long.BYTES);
            }
            BufferOutputWriterMirror.this.flush(entries);
            if (endOfOutput == false) {
                BufferOutputWriterMirror.this.ensure();
            }
        }
    }
}
