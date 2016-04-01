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
package com.asakusafw.m3bp.mirror.jna;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Test for {@link MoveEdgeProcessor}.
 */
public class MoveEdgeProcessorTest {

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        MoveEdgeProcessor proc = new MoveEdgeProcessor();
        proc.add(BufferUtil.output(4, b -> b.putInt(100)));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(collect(inputs), is(Arrays.asList(100)));
    }

    /**
     * multiple records.
     * @throws Exception if failed
     */
    @Test
    public void multiple_records() throws Exception {
        MoveEdgeProcessor proc = new MoveEdgeProcessor();
        proc.add(BufferUtil.output(12, b -> b.putInt(100), b -> b.putInt(200), b -> b.putInt(300)));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(collect(inputs), containsInAnyOrder(100, 200, 300));
    }

    /**
     * multiple buffers.
     * @throws Exception if failed
     */
    @Test
    public void multiple_buffers() throws Exception {
        MoveEdgeProcessor proc = new MoveEdgeProcessor();
        proc.add(BufferUtil.output(4, b -> b.putInt(100)));
        proc.add(BufferUtil.output(8, b -> b.putInt(200), b -> b.putInt(300)));
        proc.add(BufferUtil.output(12, b -> b.putInt(400), b -> b.putInt(500), b -> b.putInt(600)));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(collect(inputs), containsInAnyOrder(100, 200, 300, 400, 500, 600));
    }

    private List<Integer> collect(List<InputBufferCursor> inputs) {
        List<Integer> results = new ArrayList<>();
        inputs.stream().forEach(c -> BufferUtil.collect(c, b -> {
            assertThat(b.remaining(), is(4));
            results.add(b.getInt());
        }));
        return results;
    }
}
