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
package com.asakusafw.m3bp.bridge;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

import com.asakusafw.m3bp.mirror.MockInputReaderMirror;
import com.asakusafw.m3bp.mirror.MockPageDataInput;

/**
 * Test for {@link ValueReaderBridge}.
 */
public class ValueReaderBridgeTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        try (ValueReaderBridge bridge = new ValueReaderBridge(
                new MockInputReaderMirror(new MockPageDataInput(
                        bytes(1),
                        ints(0, 4))),
                new IntSerDe())) {
            assertThat(bridge.nextObject(), is(true));
            assertThat(bridge.getObject(), is(1));

            assertThat(bridge.nextObject(), is(false));
        }
    }

    /**
     * multiple entries.
     * @throws Exception if failed
     */
    @Test
    public void multiple_entries() throws Exception {
        try (ValueReaderBridge bridge = new ValueReaderBridge(
                new MockInputReaderMirror(new MockPageDataInput(
                        bytes(1, 2, 3),
                        ints(0, 4, 8, 12))),
                new IntSerDe())) {
            assertThat(bridge.nextObject(), is(true));
            assertThat(bridge.getObject(), is(1));

            assertThat(bridge.nextObject(), is(true));
            assertThat(bridge.getObject(), is(2));

            assertThat(bridge.nextObject(), is(true));
            assertThat(bridge.getObject(), is(3));

            assertThat(bridge.nextObject(), is(false));
        }
    }

    /**
     * empty entries.
     * @throws Exception if failed
     */
    @Test
    public void empty_entries() throws Exception {
        try (ValueReaderBridge bridge = new ValueReaderBridge(
                new MockInputReaderMirror(new MockPageDataInput(
                        bytes(),
                        ints())),
                new IntSerDe())) {
            assertThat(bridge.nextObject(), is(false));
        }
    }

    private int[] ints(int... values) {
        return values;
    }

    private byte[] bytes(int... values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 4);
        buf.order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(values);
        return buf.array();
    }
}
