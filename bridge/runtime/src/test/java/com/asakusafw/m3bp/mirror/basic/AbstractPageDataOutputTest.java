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
package com.asakusafw.m3bp.mirror.basic;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.asakusafw.m3bp.mirror.MockPageDataInput;
import com.asakusafw.m3bp.mirror.MockPageDataOutput;

/**
 * Test for {@link AbstractPageDataOutput}.
 */
public class AbstractPageDataOutputTest {

    /**
     * boolean.
     * @throws Exception if failed
     */
    @Test
    public void write_boolean() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeBoolean(false);
        out.writeBoolean(true);
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 0, 1 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 2 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_byte() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeByte(1);
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 1 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 1 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_short() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeShort(0x0123);
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 0x01, 0x23 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 2 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_char() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeChar('\u3042');
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 0x30, 0x42 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 2 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_int() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeInt(0x01234567);
        out.endPage();

        assertThat(out.getContents(), is(intBytes(0x1234567)));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * long.
     * @throws Exception if failed
     */
    @Test
    public void write_long() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeLong(0x0123456789abcdefL);
        out.endPage();

        assertThat(out.getContents(), is(longBytes(0x0123456789abcdefL)));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 8 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * float.
     * @throws Exception if failed
     */
    @Test
    public void write_float() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeFloat(3.14f);
        out.endPage();

        assertThat(out.getContents(), is(intBytes(Float.floatToIntBits(3.14f))));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * double.
     * @throws Exception if failed
     */
    @Test
    public void write_double() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeDouble(3.14);
        out.endPage();

        assertThat(out.getContents(), is(longBytes(Double.doubleToLongBits(3.14))));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 8 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * string bytes.
     * @throws Exception if failed
     */
    @Test
    public void write_string_bytes() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeBytes("Hello");
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 'H', 'e', 'l', 'l', 'o' }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 5 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * string chars.
     * @throws Exception if failed
     */
    @Test
    public void write_string_chars() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeChars("Hello");
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 0, 'H', 0, 'e', 0, 'l', 0, 'l', 0, 'o' }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 10 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * string UTF.
     * @throws Exception if failed
     */
    @Test
    public void write_string_utf() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.writeUTF("Hello, world!");
        out.endPage();

        MockPageDataInput in = new MockPageDataInput(out.getContents(), out.getPageOffsets());
        assertThat(in.next(), is(true));
        assertThat(in.readUTF(), is("Hello, world!"));
        assertThat(in.hasRemaining(), is(false));
        assertThat(in.next(), is(false));
    }

    /**
     * byte by int.
     * @throws Exception if failed
     */
    @Test
    public void write_byte_by_int() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.write(0x01234567);
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 0x67 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 1 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void write_bytes() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.write(new byte[] { 0, 1, 2, 3, 4, 0, }, 1, 4);
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 1, 2, 3, 4, }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * w/ key.
     * @throws Exception if failed
     */
    @Test
    public void key() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        out.write(1);
        out.endKey();
        out.write(2);
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 1, 2, }));
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
        MockPageDataOutput out = new MockPageDataOutput(0.5f, 0, 10, 100) {
            @Override
            protected void doFlush(boolean end) throws IOException {
                flushed.set(true);
                assertThat(getContentsInFlush(), is(new byte[] { 0, 0, 0, 1, 0, 0, 0, 2 }));
                assertThat(getPageOffsetsInFlush(), is(new int[] { 0, 4, 8 }));
                reset(0, 100, 100);
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

        assertThat(out.getContents(), is(new byte[] { 0, 0, 0, 3 }));
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
        MockPageDataOutput out = new MockPageDataOutput(0.9f, 0, 1000, 2) {
            @Override
            protected void doFlush(boolean end) throws IOException {
                flushed.set(true);
                assertThat(getContentsInFlush(), is(new byte[] { 0, 0, 0, 1, 0, 0, 0, 2 }));
                assertThat(getPageOffsetsInFlush(), is(new int[] { 0, 4, 8 }));
                reset(0, 100, 100);
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

        assertThat(out.getContents(), is(new byte[] { 0, 0, 0, 3 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * flush by manually.
     * @throws Exception if failed
     */
    @Test
    public void flush_by_manual() throws Exception {
        AtomicBoolean flushed = new AtomicBoolean(false);
        MockPageDataOutput out = new MockPageDataOutput() {
            @Override
            protected void doFlush(boolean end) throws IOException {
                flushed.set(true);
                assertThat(getContentsInFlush(), is(new byte[] { 0, 0, 0, 1, 0, 0, 0, 2 }));
                assertThat(getPageOffsetsInFlush(), is(new int[] { 0, 4, 8 }));
                reset(0, 100, 100);
            }
        };
        out.writeInt(1);
        out.endPage();
        assertThat(flushed.get(), is(false));

        out.writeInt(2);
        out.endPage();
        assertThat(flushed.get(), is(false));

        out.flush(false);
        assertThat(flushed.get(), is(true));

        out.writeInt(3);
        out.endPage();

        assertThat(out.getContents(), is(new byte[] { 0, 0, 0, 3 }));
        assertThat(out.getPageOffsets(), is(new int[] { 0, 4 }));
        assertThat(out.getKeyLengths(), is(new int[0]));
    }

    /**
     * flush by manually.
     * @throws Exception if failed
     */
    @Test
    public void flush_empty() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput() {
            @Override
            protected void doFlush(boolean end) throws IOException {
                throw new AssertionError();
            }
        };
        out.flush(true);
        // ok.
    }

    private byte[] intBytes(int value) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(value);
        buf.flip();
        return buf.array();
    }

    private byte[] longBytes(long value) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(value);
        buf.flip();
        return buf.array();
    }
}
