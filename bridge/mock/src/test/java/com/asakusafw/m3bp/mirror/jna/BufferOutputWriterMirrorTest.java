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
package com.asakusafw.m3bp.mirror.jna;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import com.asakusafw.m3bp.mirror.PageDataOutput;
import com.sun.jna.Memory;

/**
 * Test for {@link BufferOutputWriterMirror}.
 */
public class BufferOutputWriterMirrorTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        Sink sink = new Sink();
        try (BufferOutputWriterMirror mirror = output(false, sink)) {
            PageDataOutput output = mirror.getOutput();
            output.writeInt(100);
            output.endPage();
        }

        assertThat(sink.keys, hasSize(0));
        assertThat(sink.values, hasSize(1));
        assertThat(ints(sink.values), is(new int[] { 100 }));
    }

    /**
     * multiple records.
     * @throws Exception if failed
     */
    @Test
    public void multiple_records() throws Exception {
        Sink sink = new Sink();
        try (BufferOutputWriterMirror mirror = output(false, sink)) {
            PageDataOutput output = mirror.getOutput();
            output.writeInt(100);
            output.endPage();
            output.writeInt(200);
            output.endPage();
            output.writeInt(300);
            output.endPage();
        }

        assertThat(sink.keys, hasSize(0));
        assertThat(sink.values, hasSize(3));
        assertThat(ints(sink.values), is(new int[] { 100, 200, 300 }));
    }

    /**
     * keys and values.
     * @throws Exception if failed
     */
    @Test
    public void kv() throws Exception {
        Sink sink = new Sink();
        try (BufferOutputWriterMirror mirror = output(true, sink)) {
            PageDataOutput output = mirror.getOutput();
            output.writeInt(1);
            output.endKey();
            output.writeInt(100);
            output.endPage();
        }

        assertThat(sink.keys, hasSize(1));
        assertThat(ints(sink.keys), is(new int[] { 1 }));
        assertThat(sink.values, hasSize(1));
        assertThat(ints(sink.values), is(new int[] { 100 }));
    }

    /**
     * keys and values.
     * @throws Exception if failed
     */
    @Test
    public void multiple_kv() throws Exception {
        Sink sink = new Sink();
        try (BufferOutputWriterMirror mirror = output(true, sink)) {
            PageDataOutput output = mirror.getOutput();
            output.writeInt(1);
            output.endKey();
            output.writeInt(100);
            output.endPage();

            output.writeInt(2);
            output.endKey();
            output.writeInt(200);
            output.writeInt(300);
            output.endPage();

            output.writeInt(3);
            output.endKey();
            output.writeInt(400);
            output.writeInt(500);
            output.writeInt(600);
            output.endPage();
        }

        assertThat(sink.keys, hasSize(3));
        assertThat(ints(sink.keys), is(new int[] { 1, 2, 3, }));
        assertThat(sink.values, hasSize(3));
        assertThat(ints(sink.values.get(0)), is(new int[] { 100 }));
        assertThat(ints(sink.values.get(1)), is(new int[] { 200, 300 }));
        assertThat(ints(sink.values.get(2)), is(new int[] { 400, 500, 600 }));
    }

    private BufferOutputWriterMirror output(boolean key, Sink sink) {
        return new BufferOutputWriterMirror(() -> new OutputBufferFragment(1024, 16, key), sink);
    }

    private int[] ints(ByteBuffer buffer) {
        int[] results = new int[buffer.remaining() / Integer.BYTES];
        for (int i = 0; i < results.length; i++) {
            results[i] = buffer.getInt();
        }
        return results;
    }

    private int[] ints(List<ByteBuffer> buffers) {
        int[] results = new int[buffers.size()];
        for (int i = 0; i < results.length; i++) {
            ByteBuffer buffer = buffers.get(i);
            assertThat(buffer.remaining(), is(Integer.BYTES));
            results[i] = buffer.getInt();
        }
        return results;
    }

    private static class Sink implements Consumer<OutputBufferFragment> {

        final List<ByteBuffer> keys = new ArrayList<>();

        final List<ByteBuffer> values = new ArrayList<>();

        Sink() {
            return;
        }

        @Override
        public void accept(OutputBufferFragment fragment) {
            if (fragment.getEntryCount() == 0) {
                return;
            }
            Memory offsets = fragment.getEntryOffsets();
            Memory contents = fragment.getContents();
            Memory keyLengths = fragment.hasKey() ? fragment.getKeyLengths() : null;
            for (int i = 0; i < fragment.getEntryCount(); i++) {
                long start = offsets.getLong(i * Long.BYTES);
                if (keyLengths != null) {
                    long keyLen = keyLengths.getLong(i * Long.BYTES);
                    keys.add(contents.getByteBuffer(start, keyLen));
                    start += keyLen;
                }
                long valueEnd = offsets.getLong((i + 1) * Long.BYTES);
                values.add(contents.getByteBuffer(start, valueEnd - start));
            }
        }
    }
}
