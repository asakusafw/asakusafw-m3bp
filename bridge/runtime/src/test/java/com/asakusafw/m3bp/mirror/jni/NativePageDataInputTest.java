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
package com.asakusafw.m3bp.mirror.jni;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

import com.asakusafw.m3bp.mirror.PageDataInput;
import com.asakusafw.m3bp.mirror.basic.Procedure;

/**
 * Test for {@code NativePageDataInput}.
 */
public class NativePageDataInputTest {

    /**
     * Checks native library is enabled.
     */
    @ClassRule
    public static final NativeEnabled NATIVE = new NativeEnabled();

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
        with(createFromShort(0x0102), in -> {
            assertThat(in.readShort(), is((short) 0x0102));
        });
    }

    /**
     * unsigned short.
     * @throws Exception if failed
     */
    @Test
    public void read_ushort() throws Exception {
        with(createFromShort(0x89ab), in -> {
            assertThat(in.readUnsignedShort(), is(0x89ab));
        });
    }

    /**
     * char.
     * @throws Exception if failed
     */
    @Test
    public void read_char() throws Exception {
        with(createFromShort(0x3042), in -> {
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
     * comparing regions.
     * @throws Exception if failed
     */
    @Test
    public void compare() throws Exception {
        with(create(1, 2, 3, 4), in -> {
            PageDataInput other = create(1, 2, 3, 4);
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
        NativePageDataInput input = create(new byte[0][]);
        assertThat(input.next(), is(false));
    }

    /**
     * multiple pages.
     * @throws Exception if failed
     */
    @Test
    public void multi_pages() throws Exception {
        with(create(new byte[][] {
            { 1, },
            { 2, 3, },
            { 4, 5, 6, },
            {},
        }), in -> {
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
        });
    }

    /**
     * test for paging.
     * @throws Exception if failed
     */
    @Test
    public void paging() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024)
                .order(ByteOrder.nativeOrder());
        buffer.putInt(12345);
        long address = NativeBufferUtil.getAddress(buffer);
        long page = 1L << 32;
        long length = page * 4;
        Assume.assumeThat(Long.compareUnsigned(address + length, address), greaterThan(0));

        NativeDataInput in = new NativeDataInput(address, length);
        in.region(page * 1, page);

        assertThat(in.toString(), in.hasRemaining(), is(true));
        assertThat(in.toString(), in.remaining(), is(page));

        in.region(0, Integer.BYTES);
        assertThat(in.toString(), in.hasRemaining(), is(true));
        assertThat(in.toString(), in.remaining(), is((long) Integer.BYTES));
        assertThat(in.toString(), in.readInt(), is(12345));
    }

    /**
     * test for feed-forward views.
     * @throws Exception if failed
     */
    @Test
    public void feed() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024)
                .order(ByteOrder.nativeOrder());
        buffer.putInt(12345);
        buffer.putInt(67890);
        long address = NativeBufferUtil.getAddress(buffer);
        long offset = NativeDataInput.BUFFER_CHUNK_LIMIT - Integer.BYTES;
        long page = 1L << 32;
        long length = page * 4;
        Assume.assumeThat(Long.compareUnsigned(address, address - offset), greaterThan(0));
        Assume.assumeThat(Long.compareUnsigned(address - offset + length, address - offset), greaterThan(0));

        NativeDataInput in = new NativeDataInput(address - offset, length);

        in.region(offset, Integer.BYTES * 2);
        assertThat(in.toString(), in.position(), is(offset + Integer.BYTES * 0));
        assertThat(in.toString(), in.remaining(), is((long) Integer.BYTES * 2));

        assertThat(in.toString(), in.readInt(), is(12345));
        assertThat(in.toString(), in.position(), is(offset + Integer.BYTES * 1));
        assertThat(in.toString(), in.remaining(), is((long) Integer.BYTES * 1));

        assertThat(in.toString(), in.readInt(), is(67890));
        assertThat(in.toString(), in.position(), is(offset + Integer.BYTES * 2));
        assertThat(in.toString(), in.remaining(), is((long) Integer.BYTES * 0));

    }

    private static void with(PageDataInput in, Procedure<PageDataInput, IOException> proc) throws IOException {
        assertThat(in.next(), is(true));
        proc.execute(in);
        assertThat(in.hasRemaining(), is(false));
        assertThat(in.next(), is(false));
    }

    private static NativePageDataInput create(int... bytes) {
        byte[] results = new byte[bytes.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = (byte) bytes[i];
        }
        return create(results);
    }

    private static NativePageDataInput createFromShort(int value) {
        ByteBuffer buf = ByteBuffer.allocate(Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .putShort((short) value);
        buf.flip();
        return create(buf.array());
    }

    private static NativePageDataInput createFromInt(int value) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES)
            .order(ByteOrder.nativeOrder())
            .putInt(value);
        buf.flip();
        return create(buf.array());
    }

    private static NativePageDataInput createFromLong(long value) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES)
                .order(ByteOrder.nativeOrder())
            .putLong(value);
        buf.flip();
        return create(buf.array());
    }

    private static NativePageDataInput create(byte[] bytes) {
        return create(new byte[][] { bytes });
    }

    private static NativePageDataInput create(byte[][] pages) {
        int size = Arrays.stream(pages).mapToInt(it -> it.length).sum();
        ByteBuffer data = ByteBuffer.allocateDirect(size)
                .order(ByteOrder.nativeOrder());
        ByteBuffer entries = ByteBuffer.allocateDirect(Long.BYTES * (pages.length + 1))
                .order(ByteOrder.nativeOrder());
        entries.putLong(0);
        for (byte[] bytes : pages) {
            data.put(bytes);
            entries.putLong(data.position());
        }
        return new NativePageDataInput(NativeBufferUtil.getAddress(data),
                new NativeDataInput(NativeBufferUtil.getAddress(entries), entries.capacity()));
    }
}
