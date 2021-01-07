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
package com.asakusafw.m3bp.mirror.jni;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * Test for {@code NativeBufferUtil}.
 */
public class NativeBufferUtilTest {

    /**
     * Checks native library is enabled.
     */
    @ClassRule
    public static final NativeEnabled NATIVE = new NativeEnabled();

    /**
     * test about views.
     */
    @Test
    public void view() {
        byte[] contents = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, };

        ByteBuffer buffer = ByteBuffer.allocateDirect(contents.length);
        buffer.put(contents);
        buffer.clear();

        long address = NativeBufferUtil.getAddress(buffer);
        ByteBuffer view = NativeBufferUtil.getView(address, contents.length);
        assertThat(view.capacity(), is(contents.length));
        assertThat(NativeBufferUtil.getAddress(view), is(address));

        byte[] data = new byte[contents.length];
        view.get(data);
        view.clear();
        assertThat(data, is(contents));
    }

    /**
     * test about compare.
     */
    @Test
    public void compare() {
        byte[] contents = new byte[] { 1, 2, 3, 4, 5, 2, 3, 4, -1, };

        ByteBuffer buffer = ByteBuffer.allocateDirect(contents.length);
        buffer.put(contents);
        buffer.clear();

        long address = NativeBufferUtil.getAddress(buffer);

        assertThat(NativeBufferUtil.compare(address, address, contents.length), is(0));

        // (2, 3, 4) == (2, 3, 4)
        assertThat(NativeBufferUtil.compare(address + 1, address + 5, 3), is(0));

        // (2, 3, 4) < (3, 4, 5)
        assertThat(NativeBufferUtil.compare(address + 1, address + 2, 3), lessThan(0));

        // (2, 3, 4) > (1, 2, 3)
        assertThat(NativeBufferUtil.compare(address + 1, address + 0, 3), greaterThan(0));

        // (+1) < (255) - unsigned
        assertThat(NativeBufferUtil.compare(address + 0, address + 8, 1), lessThan(0));
    }
}
