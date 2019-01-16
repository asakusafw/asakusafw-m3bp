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
package com.asakusafw.m3bp.mirror.unsafe;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.ClassRule;
import org.junit.Test;

import com.asakusafw.m3bp.mirror.PageDataInput;
import com.asakusafw.m3bp.mirror.basic.Procedure;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Test for {@link UnsafePageDataInput}.
 */
public class UnsafePageDataInputTest {

    /**
     * check unsafe API.
     */
    @ClassRule
    public static final UnsafeChecker UNSAFE_CHECKER = new UnsafeChecker();

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
        with(createFromShort((short) 0x0102), in -> {
            assertThat(in.readShort(), is((short) 0x0102));
        });
    }

    /**
     * unsigned short.
     * @throws Exception if failed
     */
    @Test
    public void read_ushort() throws Exception {
        with(createFromShort((short) 0x89ab), in -> {
            assertThat(in.readUnsignedShort(), is(0x89ab));
        });
    }

    /**
     * char.
     * @throws Exception if failed
     */
    @Test
    public void read_char() throws Exception {
        with(createFromShort((short) '\u3042'), in -> {
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
            Mock other = create(1, 2, 3, 4);
            assertThat(other.next(), is(true));
            assertThat(in.comparePage(other), is(0));
            in.skipBytes(4);
        });
    }

    /**
     * comparing slice.
     * @throws Exception if failed
     */
    @Test
    public void compare_long() throws Exception {
        with(create(0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3), in -> {
            Mock other = create(0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3);
            assertThat(other.next(), is(true));
            assertThat(in.comparePage(other), is(0));
            in.skipBytes(20);
        });
    }

    /**
     * w/ empty contents.
     * @throws Exception if failed
     */
    @Test
    public void empty_contents() throws Exception {
        Mock in = new Mock(new byte[0], new int[0]);
        assertThat(in.next(), is(false));
    }

    /**
     * multiple pages.
     * @throws Exception if failed
     */
    @Test
    public void multi_pages() throws Exception {
        Mock in = new Mock(
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
        Mock in = new Mock(new byte[] { 1 }, new int[] { 0, 1 }) {
            @Override
            protected boolean advance() {
                if (advanced.compareAndSet(false, true) == false) {
                    return false;
                }
                reset(new byte[] { 2 }, new int[] { 0, 1 });
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

    private Mock create(int... bytes) {
        byte[] results = new byte[bytes.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = (byte) bytes[i];
        }
        return create(results);
    }

    private Mock createFromShort(short value) {
        ByteBuffer buf = ByteBuffer.allocate(Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .putShort(value);
        buf.flip();
        return create(buf.array());
    }

    private Mock createFromInt(int value) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES)
                .order(ByteOrder.nativeOrder())
                .putInt(value);
        buf.flip();
        return create(buf.array());
    }

    private Mock createFromLong(long value) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES)
                .order(ByteOrder.nativeOrder())
                .putLong(value);
        buf.flip();
        return create(buf.array());
    }

    private Mock create(byte[] bytes) {
        Mock in = new Mock(bytes, new int[] { 0, bytes.length });
        return in;
    }

    private static class Mock extends UnsafePageDataInput {

        private Memory contents;

        private Memory offsets;

        Mock(byte[] contents, int[] offsets) {
            reset(contents, offsets);
        }

        final void reset(byte[] newContents, int[] newOffsets) {
            this.contents = new Memory(Math.max(newContents.length, 1));
            this.offsets = new Memory(Math.max(newOffsets.length * Long.BYTES, 1));
            for (int i = 0; i < newContents.length; i++) {
                this.contents.setByte(i * Byte.BYTES, newContents[i]);
            }
            for (int i = 0; i < newOffsets.length; i++) {
                this.offsets.setLong(i * Long.BYTES, newOffsets[i]);
            }
            reset(
                    Pointer.nativeValue(contents),
                    Pointer.nativeValue(offsets),
                    Pointer.nativeValue(offsets) + newOffsets.length * Long.BYTES);
        }
    }
}
