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
package com.asakusafw.m3bp.bridge;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import com.asakusafw.dag.api.processor.GroupReader.GroupInfo;
import com.asakusafw.m3bp.mirror.MockInputReaderMirror;
import com.asakusafw.m3bp.mirror.MockPageDataInput;

/**
 * Test for {@link KeyValueReaderBridge}.
 */
public class KeyValueReaderBridgeTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        try (KeyValueReaderBridge reader = new KeyValueReaderBridge(
                new MockInputReaderMirror(
                        new MockPageDataInput(bytes(1), ints(0, 4)),
                        new MockPageDataInput(bytes(2), ints(0, 4))),
                new IntKeyValueSerDe())) {
            assertThat(reader.nextGroup(), is(true));
            assertThat(reader.getGroup(), is(group(1)));
            assertThat(reader.nextObject(), is(true));
            assertThat(reader.getObject(), is(2));
            assertThat(reader.nextObject(), is(false));

            assertThat(reader.nextGroup(), is(false));
        }
    }

    /**
     * multiple values.
     * @throws Exception if failed
     */
    @Test
    public void multiple_values() throws Exception {
        try (KeyValueReaderBridge reader = new KeyValueReaderBridge(
                new MockInputReaderMirror(
                        new MockPageDataInput(bytes(1), ints(0, 4)),
                        new MockPageDataInput(bytes(2, 3, 4), ints(0, 12))),
                new IntKeyValueSerDe())) {
            assertThat(reader.nextGroup(), is(true));
            assertThat(reader.getGroup(), is(group(1)));
            assertThat(reader.nextObject(), is(true));
            assertThat(reader.getObject(), is(2));
            assertThat(reader.nextObject(), is(true));
            assertThat(reader.getObject(), is(3));
            assertThat(reader.nextObject(), is(true));
            assertThat(reader.getObject(), is(4));
            assertThat(reader.nextObject(), is(false));

            assertThat(reader.nextGroup(), is(false));
        }
    }

    /**
     * multiple groups.
     * @throws Exception if failed
     */
    @Test
    public void multiple_groups() throws Exception {
        try (KeyValueReaderBridge reader = new KeyValueReaderBridge(
                new MockInputReaderMirror(
                        new MockPageDataInput(bytes(1, 2, 3), ints(0, 4, 8, 12)),
                        new MockPageDataInput(bytes(4, 5, 6), ints(0, 4, 8, 12))),
                new IntKeyValueSerDe())) {
            assertThat(reader.nextGroup(), is(true));
            assertThat(reader.getGroup(), is(group(1)));
            assertThat(reader.nextObject(), is(true));
            assertThat(reader.getObject(), is(4));
            assertThat(reader.nextObject(), is(false));

            assertThat(reader.nextGroup(), is(true));
            assertThat(reader.getGroup(), is(group(2)));
            assertThat(reader.nextObject(), is(true));
            assertThat(reader.getObject(), is(5));
            assertThat(reader.nextObject(), is(false));

            assertThat(reader.nextGroup(), is(true));
            assertThat(reader.getGroup(), is(group(3)));
            assertThat(reader.nextObject(), is(true));
            assertThat(reader.getObject(), is(6));
            assertThat(reader.nextObject(), is(false));

            assertThat(reader.nextGroup(), is(false));
        }
    }

    /**
     * empty groups.
     * @throws Exception if failed
     */
    @Test
    public void empty_groups() throws Exception {
        try (KeyValueReaderBridge reader = new KeyValueReaderBridge(
                new MockInputReaderMirror(
                        new MockPageDataInput(bytes(), ints()),
                        new MockPageDataInput(bytes(), ints())),
                new IntKeyValueSerDe())) {
            assertThat(reader.nextGroup(), is(false));
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

    private Matcher<GroupInfo> group(int key) {
        return new BaseMatcher<GroupInfo>() {
            @Override
            public boolean matches(Object item) {
                try {
                    return ((GroupInfo) item).getValue().equals(key);
                } catch (Exception e) {
                    return false;
                }
            }
            @Override
            public void describeTo(Description description) {
                description.appendText("group ").appendValue(key);
            }
        };
    }
}
