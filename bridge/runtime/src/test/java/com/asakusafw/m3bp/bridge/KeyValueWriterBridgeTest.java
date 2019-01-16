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
package com.asakusafw.m3bp.bridge;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.asakusafw.m3bp.mirror.MockPageDataInput;
import com.asakusafw.m3bp.mirror.MockPageDataOutput;

/**
 * Test for {@link KeyValueWriterBridge}.
 */
public class KeyValueWriterBridgeTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        try (KeyValueWriterBridge bridge = new KeyValueWriterBridge(() -> out, new IntKeyValueSerDe(0, 100))) {
            bridge.putObject(1);
            check(out, new int[][] {
                {1, 101},
            });
        }
    }

    /**
     * multiple entries.
     * @throws Exception if failed
     */
    @Test
    public void multiple_entries() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        try (KeyValueWriterBridge bridge = new KeyValueWriterBridge(() -> out, new IntKeyValueSerDe(0, 100))) {
            bridge.putObject(1);
            bridge.putObject(2);
            bridge.putObject(3);
            check(out, new int[][] {
                {1, 101},
                {2, 102},
                {3, 103},
            });
        }
    }

    /**
     * empty entries.
     * @throws Exception if failed
     */
    @Test
    public void empty_entries() throws Exception {
        MockPageDataOutput out = new MockPageDataOutput();
        try (KeyValueWriterBridge bridge = new KeyValueWriterBridge(() -> out, new IntKeyValueSerDe(0, 100))) {
            check(out, new int[][] {
                // empty
            });
        }
    }

    private void check(MockPageDataOutput out, int[][] pairs) throws IOException, InterruptedException {
        int[] keyLengths = out.getKeyLengths();
        assertThat(keyLengths.length, is(pairs.length));
        for (int i = 0; i < pairs.length; i++) {
            assertThat("each key length = 4", keyLengths[i], is(4));
        }
        IntSerDe sd = new IntSerDe();
        MockPageDataInput in = new MockPageDataInput(out.getContents(), out.getPageOffsets());
        for (int[] pair : pairs) {
            assertThat(pair.length, is(2));
            assertThat(in.next(), is(true));
            assertThat(in.hasRemaining(), is(true));
            assertThat(sd.deserialize(in), is(pair[0]));
            assertThat(in.hasRemaining(), is(true));
            assertThat(sd.deserialize(in), is(pair[1]));
            assertThat(in.hasRemaining(), is(false));
        }
        assertThat(in.next(), is(false));
    }
}
