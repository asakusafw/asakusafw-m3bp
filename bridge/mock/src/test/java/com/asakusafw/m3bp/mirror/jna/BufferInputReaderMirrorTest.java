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
package com.asakusafw.m3bp.mirror.jna;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import org.junit.Test;

import com.asakusafw.lang.utils.common.AssertUtil;
import com.asakusafw.lang.utils.common.Suppliers;
import com.asakusafw.m3bp.mirror.PageDataInput;

/**
 * Test for {@link BufferInputReaderMirror}.
 */
public class BufferInputReaderMirrorTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        InputBufferCursor cursor = new InputBufferCursor(
                Suppliers.of(BufferUtil.input(4, b -> b.putInt(100))));
        try (BufferInputReaderMirror mirror = new BufferInputReaderMirror(cursor)) {
            AssertUtil.catching(() -> mirror.getKeyInput());

            PageDataInput values = mirror.getValueInput();
            assertThat(values.next(), is(true));
            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(100));
            assertThat(values.hasRemaining(), is(false));

            assertThat(values.next(), is(false));
        }
    }

    /**
     * multiple records in the page.
     * @throws Exception if failed
     */
    @Test
    public void multiple_records() throws Exception {
        InputBufferCursor cursor = new InputBufferCursor(
                Suppliers.of(BufferUtil.input(8, b -> b.putInt(100), b -> b.putInt(200))));
        try (BufferInputReaderMirror mirror = new BufferInputReaderMirror(cursor)) {
            AssertUtil.catching(() -> mirror.getKeyInput());

            PageDataInput values = mirror.getValueInput();
            assertThat(values.next(), is(true));
            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(100));
            assertThat(values.hasRemaining(), is(false));

            assertThat(values.next(), is(true));
            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(200));
            assertThat(values.hasRemaining(), is(false));

            assertThat(values.next(), is(false));
        }
    }

    /**
     * multiple pages.
     * @throws Exception if failed
     */
    @Test
    public void multiple_pages() throws Exception {
        InputBufferCursor cursor = new InputBufferCursor(
                Suppliers.of(BufferUtil.input(4, b -> b.putInt(100)),
                        BufferUtil.input(4, b -> b.putInt(200))));
        try (BufferInputReaderMirror mirror = new BufferInputReaderMirror(cursor)) {
            AssertUtil.catching(() -> mirror.getKeyInput());

            PageDataInput values = mirror.getValueInput();
            assertThat(values.next(), is(true));
            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(100));
            assertThat(values.hasRemaining(), is(false));

            assertThat(values.next(), is(true));
            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(200));
            assertThat(values.hasRemaining(), is(false));

            assertThat(values.next(), is(false));
        }
    }

    /**
     * keys and values.
     * @throws Exception if failed
     */
    @Test
    public void kv() throws Exception {
        InputBufferCursor cursor = new InputBufferCursor(
                Suppliers.of(
                        BufferUtil.input(1, b -> b.put((byte) 0)),
                        BufferUtil.input(1, b -> b.put((byte) 1))),
                Suppliers.of(
                        BufferUtil.input(8, b -> b.putInt(100)),
                        BufferUtil.input(8, b -> { b.putInt(200); b.putInt(300); })));
        try (BufferInputReaderMirror mirror = new BufferInputReaderMirror(cursor)) {
            PageDataInput keys = mirror.getKeyInput();
            PageDataInput values = mirror.getValueInput();

            assertThat(keys.next(), is(true));
            assertThat(values.next(), is(true));

            assertThat(keys.hasRemaining(), is(true));
            assertThat(keys.readBoolean(), is(false));
            assertThat(keys.hasRemaining(), is(false));

            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(100));
            assertThat(values.hasRemaining(), is(false));

            assertThat(keys.next(), is(true));
            assertThat(values.next(), is(true));

            assertThat(keys.hasRemaining(), is(true));
            assertThat(keys.readBoolean(), is(true));
            assertThat(keys.hasRemaining(), is(false));

            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(200));
            assertThat(values.hasRemaining(), is(true));
            assertThat(values.readInt(), is(300));
            assertThat(values.hasRemaining(), is(false));

            assertThat(keys.next(), is(false));
            assertThat(values.next(), is(false));
        }
    }
}
