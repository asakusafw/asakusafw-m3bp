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
package com.asakusafw.m3bp.bridge;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.asakusafw.m3bp.mirror.MockInputReaderMirror;
import com.asakusafw.m3bp.mirror.MockOutputWriterMirror;
import com.asakusafw.m3bp.mirror.MockPageDataInput;
import com.asakusafw.m3bp.mirror.MockPageDataOutput;

/**
 * Test for {@link ValueWriterBridge}.
 */
public class ValueWriterBridgeTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        check(1);
    }

    /**
     * multiple entries.
     * @throws Exception if failed
     */
    @Test
    public void multiple_entries() throws Exception {
        check(1, 2, 3);
    }

    /**
     * empty entries.
     * @throws Exception if failed
     */
    @Test
    public void empty_entries() throws Exception {
        check();
    }

    private void check(int... values) throws IOException, InterruptedException {
        byte[] contents;
        int[] offsets;
        MockPageDataOutput out = new MockPageDataOutput();
        try (ValueWriterBridge writer = new ValueWriterBridge(new MockOutputWriterMirror(out), new IntSerDe())) {
            for (int value : values) {
                writer.putObject(value);
            }
            contents = out.getContents();
            offsets = out.getPageOffsets();
        }
        try (ValueReaderBridge reader = new ValueReaderBridge(
                new MockInputReaderMirror(new MockPageDataInput(contents, offsets)),
                new IntSerDe())) {
            for (int value : values) {
                assertThat(reader.nextObject(), is(true));
                assertThat(reader.getObject(), is(value));
            }
            assertThat(reader.nextObject(), is(false));
        }
    }
}
