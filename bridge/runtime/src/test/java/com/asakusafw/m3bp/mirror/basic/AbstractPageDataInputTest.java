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
package com.asakusafw.m3bp.mirror.basic;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.asakusafw.m3bp.mirror.MockPageDataInput;
import com.asakusafw.m3bp.mirror.PageDataInput;

/**
 * Test for {@link AbstractPageDataInput}.
 */
public class AbstractPageDataInputTest {

    /**
     * boolean.
     * @throws Exception if failed
     */
    @Test
    public void read_boolean() throws Exception {
        with(create(0, 1), in -> {
            assertThat(in.readBoolean(), is(false));
            assertThat(in.readBoolean(), is(true));
        });
    }

    /**
     * byte.
     * @throws Exception if failed
     */
    @Test
    public void read_byte() throws Exception {
        with(create(0x12), in -> {
            assertThat(in.readByte(), is((byte) 0x12));
        });
    }

    /**
     * unsigned byte.
     * @throws Exception if failed
     */
    @Test
    public void read_ubyte() throws Exception {
        with(create(0x89), in -> {
            assertThat(in.readUnsignedByte(), is(0x89));
        });
    }

    /**
     * short.
     * @throws Exception if failed
     */
    @Test
    public void read_short() throws Exception {
        with(create(0x01, 0x02), in -> {
            assertThat(in.readShort(), is((short) 0x0102));
        });
    }

    /**
     * unsigned short.
     * @throws Exception if failed
     */
    @Test
    public void read_ushort() throws Exception {
        with(create(0x89, 0xab), in -> {
            assertThat(in.readUnsignedShort(), is(0x89ab));
        });
    }

    /**
     * char.
     * @throws Exception if failed
     */
    @Test
    public void read_char() throws Exception {
        with(create(0x30, 0x42), in -> {
            assertThat(in.readChar(), is('\u3042'));
        });
    }

    /**
     * int.
     * @throws Exception if failed
     */
    @Test
    public void read_int() throws Exception {
        with(createFromInt(0x01234567), in -> {
            assertThat(in.readInt(), is(0x01234567));
        });
    }

    /**
     * long.
     * @throws Exception if failed
     */
    @Test
    public void read_long() throws Exception {
        with(createFromLong(0x0123_4567_89ab_cdefL), in -> {
            assertThat(in.readLong(), is(0x0123_4567_89ab_cdefL));
        });
    }

    /**
     * float.
     * @throws Exception if failed
     */
    @Test
    public void read_float() throws Exception {
        with(createFromInt(Float.floatToIntBits(3.14f)), in -> {
            assertThat(in.readFloat(), is(3.14f));
        });
    }

    /**
     * double.
     * @throws Exception if failed
     */
    @Test
    public void read_double() throws Exception {
        with(createFromLong(Double.doubleToLongBits(3.14)), in -> {
            assertThat(in.readDouble(), is(3.14));
        });
    }

    /**
     * skip.
     * @throws Exception if failed
     */
    @Test
    public void skip_bytes() throws Exception {
        with(create(0, 1, 0, 0, 2, 3, 0), in -> {
            assertThat(in.skipBytes(1), is(1));
            assertThat(in.readByte(), is((byte) 1));
            assertThat(in.skipBytes(2), is(2));
            assertThat(in.readByte(), is((byte) 2));
            assertThat(in.readByte(), is((byte) 3));
            assertThat(in.skipBytes(2), is(1));
            assertThat(in.skipBytes(2), is(0));
        });
    }

    /**
     * read fully.
     * @throws Exception if failed
     */
    @Test
    public void read_fully() throws Exception {
        with(create(1, 2, 3, 4, -1), in -> {
            byte[] buf = new byte[6];
            in.readFully(buf, 1, 4);
            assertThat(buf, is(new byte[] { 0, 1, 2, 3, 4, 0 }));
            assertThat(in.readByte(), is((byte) -1));
        });
    }

    /**
     * comparing slice.
     * @throws Exception if failed
     */
    @Test
    public void compare() throws Exception {
        with(create(1, 2, 3, 4), in -> {
            MockPageDataInput other = create(1, 2, 3, 4);
            assertThat(other.next(), is(true));
            assertThat(in.comparePage(other), is(0));
            in.skipBytes(4);
        });
    }

    /**
     * w/ empty contents.
     * @throws Exception if failed
     */
    @Test
    public void empty_contents() throws Exception {
        MockPageDataInput in = new MockPageDataInput(new byte[0], new int[0]);
        assertThat(in.next(), is(false));
    }

    /**
     * multiple pages.
     * @throws Exception if failed
     */
    @Test
    public void multi_pages() throws Exception {
        MockPageDataInput in = new MockPageDataInput(
                new byte[] {
                        1,
                        2, 3,
                        4, 5, 6,
                        // empty page
                },
                new int[] { 0, 1, 3, 6, 6, });

        assertThat(in.next(), is(true));
        assertThat(in.readByte(), is((byte) 1));
        assertThat(in.hasRemaining(), is(false));

        assertThat(in.next(), is(true));
        assertThat(in.readByte(), is((byte) 2));
        assertThat(in.readByte(), is((byte) 3));
        assertThat(in.hasRemaining(), is(false));

        assertThat(in.next(), is(true));
        assertThat(in.readByte(), is((byte) 4));
        assertThat(in.readByte(), is((byte) 5));
        assertThat(in.readByte(), is((byte) 6));
        assertThat(in.hasRemaining(), is(false));

        assertThat(in.next(), is(true));
        assertThat(in.hasRemaining(), is(false));

        assertThat(in.next(), is(false));
    }

    /**
     * advance.
     * @throws Exception if failed
     */
    @Test
    public void advance() throws Exception {
        AtomicBoolean advanced = new AtomicBoolean();
        MockPageDataInput in = new MockPageDataInput(new byte[] { 1 }, new int[] { 0, 1 }) {
            @Override
            protected boolean doAdvance() {
                if (advanced.compareAndSet(false, true) == false) {
                    return false;
                }
                reset(1234, new byte[] { 2 }, new int[] { 0, 1 });
                return true;
            }
        };
        assertThat(in.next(), is(true));
        assertThat(in.readByte(), is((byte) 1));
        assertThat(in.hasRemaining(), is(false));

        assertThat(in.next(), is(true));
        assertThat(in.readByte(), is((byte) 2));
        assertThat(in.hasRemaining(), is(false));

        assertThat(in.next(), is(false));
        assertThat(advanced.get(), is(true));
    }

    private void with(PageDataInput in, Procedure<PageDataInput, IOException> proc) throws IOException {
        assertThat(in.next(), is(true));
        proc.execute(in);
        assertThat(in.hasRemaining(), is(false));
        assertThat(in.next(), is(false));
    }

    private MockPageDataInput create(int... bytes) {
        byte[] results = new byte[bytes.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = (byte) bytes[i];
        }
        return create(results);
    }

    private MockPageDataInput createFromInt(int value) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(value);
        buf.flip();
        return create(buf.array());
    }

    private MockPageDataInput createFromLong(long value) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(value);
        buf.flip();
        return create(buf.array());
    }

    private MockPageDataInput create(byte[] bytes) {
        MockPageDataInput in = new MockPageDataInput(bytes, new int[] { 0, bytes.length });
        return in;
    }
}
