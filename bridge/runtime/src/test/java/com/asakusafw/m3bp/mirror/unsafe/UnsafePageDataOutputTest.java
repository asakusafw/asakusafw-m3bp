/**
 * Copyright 2011-2018 Asakusa Framework Team.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Test for {@link UnsafePageDataOutput}.
 */
public class UnsafePageDataOutputTest {

    /**
     * boolean.
     * @throws Exception if failed
     */
    @Test
    public void write_boolean() throws Exception {
        Mock out = new Mock();
        out.writeBoolean(false);
        out.writeBoolean(true);
        out.endPage();
        out.flush(true);

        assertThat(out.getByteContents(), is(new byte[] { 0, 1 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 2 }));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_byte() throws Exception {
        Mock out = new Mock();
        out.writeByte(1);
        out.endPage();
        out.flush(true);

        assertThat(out.getByteContents(), is(new byte[] { 1 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 1 }));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_short() throws Exception {
        Mock out = new Mock();
        out.writeShort(0x0123);
        out.endPage();
        out.flush(true);

        assertThat(out.getShortContents(), is(new short[] { 0x0123 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 2 }));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_char() throws Exception {
        Mock out = new Mock();
        out.writeChar('\u3042');
        out.endPage();
        out.flush(true);

        assertThat(out.getShortContents(), is(new short[] { '\u3042' }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 2 }));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_int() throws Exception {
        Mock out = new Mock();
        out.writeInt(0x01234567);
        out.endPage();
        out.flush(true);

        assertThat(out.getIntContents(), is(new int[] { 0x1234567 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
    }

    /**
     * long.
     * @throws Exception if failed
     */
    @Test
    public void write_long() throws Exception {
        Mock out = new Mock();
        out.writeLong(0x0123456789abcdefL);
        out.endPage();
        out.flush(true);

        assertThat(out.getLongContents(), is(new long[] { 0x0123456789abcdefL }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 8 }));
    }

    /**
     * float.
     * @throws Exception if failed
     */
    @Test
    public void write_float() throws Exception {
        Mock out = new Mock();
        out.writeFloat(3.14f);
        out.endPage();
        out.flush(true);

        assertThat(out.getIntContents(), is(new int[] { Float.floatToIntBits(3.14f) }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
    }

    /**
     * double.
     * @throws Exception if failed
     */
    @Test
    public void write_double() throws Exception {
        Mock out = new Mock();
        out.writeDouble(3.14);
        out.endPage();
        out.flush(true);

        assertThat(out.getLongContents(), is(new long[] { Double.doubleToLongBits(3.14) }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 8 }));
    }

    /**
     * string bytes.
     * @throws Exception if failed
     */
    @Test
    public void write_string_bytes() throws Exception {
        Mock out = new Mock();
        out.writeBytes("Hello");
        out.endPage();
        out.flush(true);

        assertThat(out.getByteContents(), is(new byte[] { 'H', 'e', 'l', 'l', 'o' }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 5 }));
    }

    /**
     * string chars.
     * @throws Exception if failed
     */
    @Test
    public void write_string_chars() throws Exception {
        Mock out = new Mock();
        out.writeChars("Hello");
        out.endPage();
        out.flush(true);

        assertThat(out.getShortContents(), is(new short[] { 'H', 'e', 'l', 'l', 'o' }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 10 }));
    }

    /**
     * string UTF.
     * @throws Exception if failed
     */
    @Test
    public void write_string_utf() throws Exception {
        Mock out = new Mock();
        UnsafeDataBuffer buf = new UnsafeDataBuffer();
        buf.dataPtr = out.dataPtr;

        out.writeUTF("Hello, world!");
        out.endPage();
        out.flush(true);

        assertThat(buf.readUTF(), is("Hello, world!"));
        assertThat(out.dataPtr, is(buf.dataPtr));
    }

    /**
     * byte by int.
     * @throws Exception if failed
     */
    @Test
    public void write_byte_by_int() throws Exception {
        Mock out = new Mock();
        out.write(0x01234567);
        out.endPage();
        out.flush(true);

        assertThat(out.getByteContents(), is(new byte[] { 0x67 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 1 }));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_bytes() throws Exception {
        Mock out = new Mock();
        out.write(new byte[] { 0, 1, 2, 3, 4, 0, }, 1, 4);
        out.endPage();
        out.flush(true);

        assertThat(out.getByteContents(), is(new byte[] { 1, 2, 3, 4, }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
    }

    /**
     * w/ key.
     * @throws Exception if failed
     */
    @Test
    public void key() throws Exception {
        Mock out = new Mock(.8f, 1024, 1024, 1024);
        out.write(1);
        out.endKey();
        out.write(2);
        out.endPage();

        assertThat(out.getByteContents(), is(new byte[] { 1, 2, }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 2 }));
        assertThat(out.getKeyLengths(), is(new int[] { 1 }));
    }

    /**
     * flush by exceeding content size.
     * @throws Exception if failed
     */
    @Test
    public void flush_by_contents() throws Exception {
        AtomicBoolean flushed = new AtomicBoolean(false);
        Mock out = new Mock(0.5f, 15, 1024, 0) {
            @Override
            protected void flush(boolean endOfOutput) {
                if (endOfOutput) {
                    return;
                }
                flushed.set(true);
                assertThat(getIntContents(), is(new int[] { 1, 2 }));
                assertThat(getPageOffsets(), is(new int[] { 0, 4, 8 }));
                reset();
            }
        };
        out.writeInt(1);
        out.endPage();
        assertThat(flushed.get(), is(false));

        out.writeInt(2);
        out.endPage();
        assertThat(flushed.get(), is(true));

        out.writeInt(3);
        out.endPage();
        out.flush(true);

        assertThat(out.getIntContents(), is(new int[] { 3 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * flush by exceeding record count.
     * @throws Exception if failed
     */
    @Test
    public void flush_by_records() throws Exception {
        AtomicBoolean flushed = new AtomicBoolean(false);
        Mock out = new Mock(0.8f, 1024, Long.BYTES * 3, 0) {
            @Override
            protected void flush(boolean endOfOutput) {
                if (endOfOutput) {
                    return;
                }
                flushed.set(true);
                assertThat(getIntContents(), is(new int[] { 1, 2 }));
                assertThat(getPageOffsets(), is(new int[] { 0, 4, 8 }));
                reset();
            }
        };
        out.writeInt(1);
        out.endPage();
        assertThat(flushed.get(), is(false));

        out.writeInt(2);
        out.endPage();
        assertThat(flushed.get(), is(true));

        out.writeInt(3);
        out.endPage();
        out.flush(true);

        assertThat(out.getIntContents(), is(new int[] { 3 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    private static class Mock extends UnsafePageDataOutput {

        private final Memory contents;

        private final Memory offsets;

        private final Memory keys;

        public Mock(float flush, long contentsSize, long offsetsSize, long keysSize) {
            super(flush);
            this.contents = new Memory(contentsSize);
            this.offsets = new Memory(offsetsSize);
            this.keys = keysSize == 0 ? null : new Memory(keysSize);
            reset();
        }

        public Mock() {
            this(.8f, 1024, 1024, 0);
        }

        final void reset() {
            reset(Pointer.nativeValue(contents), Pointer.nativeValue(contents) + contents.size(),
                    keys == null ? 0L : Pointer.nativeValue(keys),
                    Pointer.nativeValue(offsets),
                    offsets.size() / Long.BYTES - 1);
        }

        public ByteBuffer getContentsBuffer() {
            int count = (int) getWrittenCount();
            if (count == 0) {
                return ByteBuffer.allocate(0);
            }
            int size = (int) offsets.getLong((long) count * Long.BYTES);
            ByteBuffer buffer = contents.getByteBuffer(0, size)
                    .order(ByteOrder.nativeOrder());
            buffer.position(0);
            buffer.limit(size);
            return buffer;
        }

        public byte[] getByteContents() {
            ByteBuffer buf = getContentsBuffer();
            byte[] results = new byte[buf.remaining()];
            buf.get(results);
            return results;
        }

        public short[] getShortContents() {
            ShortBuffer buf = getContentsBuffer().asShortBuffer();
            short[] results = new short[buf.remaining()];
            buf.get(results);
            return results;
        }

        public int[] getIntContents() {
            IntBuffer buf = getContentsBuffer().asIntBuffer();
            int[] results = new int[buf.remaining()];
            buf.get(results);
            return results;
        }

        public long[] getLongContents() {
            LongBuffer buf = getContentsBuffer().asLongBuffer();
            long[] results = new long[buf.remaining()];
            buf.get(results);
            return results;
        }

        public int[] getKeyLengths() {
            if (keys == null) {
                return new int[0];
            }
            return toIntArray(keys, (int) getWrittenCount());
        }

        public int[] getPageOffsets() {
            return toIntArray(offsets, (int) getWrittenCount() + 1);
        }

        private int[] toIntArray(Memory table, int count) {
            int[] results = new int[count];
            for (int i = 0; i < results.length; i++) {
                results[i] = (int) table.getLong((long) i * Long.BYTES);
            }
            return results;
        }
    }
}
