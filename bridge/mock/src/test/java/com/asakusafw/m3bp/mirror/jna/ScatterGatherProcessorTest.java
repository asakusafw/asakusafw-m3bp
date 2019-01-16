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
package com.asakusafw.m3bp.mirror.jna;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

import com.sun.jna.Pointer;

/**
 * Test for {@link ScatterGatherProcessor}.
 */
public class ScatterGatherProcessorTest {

    private static final BufferComparator INT_COMPARATOR = new BufferComparator() {
        @Override
        public boolean compare(Pointer a, Pointer b) {
            return a.getInt(0) < b.getInt(0);
        }
    };

    /**
     * simple case.
     */
    @Test
    public void simple() {
        ScatterGatherProcessor proc = new ScatterGatherProcessor(1, null);
        proc.add(output(1, 100));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(inputs, hasSize(1));

        Map<Integer, List<Integer>> map = collect(inputs.get(0));
        assertThat(map.keySet(), containsInAnyOrder(1));
        assertThat(map, hasEntry(equalTo(1), contains(100)));
    }

    /**
     * multiple records in the key.
     */
    @Test
    public void multiple_records() {
        ScatterGatherProcessor proc = new ScatterGatherProcessor(1, null);
        proc.add(output(new int[] {
                1, 200,
                1, 300,
                1, 100,
        }));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(inputs, hasSize(1));

        Map<Integer, List<Integer>> map = collect(inputs.get(0));
        assertThat(map.keySet(), containsInAnyOrder(1));
        assertThat(map, hasEntry(equalTo(1), containsInAnyOrder(100, 200, 300)));
    }

    /**
     * multiple records in the key.
     */
    @Test
    public void multiple_groups() {
        ScatterGatherProcessor proc = new ScatterGatherProcessor(1, null);
        proc.add(output(new int[] {
                1, 100,
                2, 200,
                3, 300,
                4, 400,
        }));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(inputs, hasSize(1));

        Map<Integer, List<Integer>> map = collect(inputs.get(0));
        assertThat(map.keySet(), containsInAnyOrder(1, 2, 3, 4));
        assertThat(map, hasEntry(equalTo(1), containsInAnyOrder(100)));
        assertThat(map, hasEntry(equalTo(2), containsInAnyOrder(200)));
        assertThat(map, hasEntry(equalTo(3), containsInAnyOrder(300)));
        assertThat(map, hasEntry(equalTo(4), containsInAnyOrder(400)));
    }

    /**
     * multiple records in the key.
     */
    @Test
    public void sorted() {
        ScatterGatherProcessor proc = new ScatterGatherProcessor(1, INT_COMPARATOR);
        proc.add(output(new int[] {
                1, 200,
                1, 300,
                1, 100,
        }));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(inputs, hasSize(1));

        Map<Integer, List<Integer>> map = collect(inputs.get(0));
        assertThat(map.keySet(), containsInAnyOrder(1));
        assertThat(map, hasEntry(equalTo(1), contains(100, 200, 300)));
    }

    /**
     * multiple records in the key.
     */
    @Test
    public void multiple_upstreams() {
        ScatterGatherProcessor proc = new ScatterGatherProcessor(1, null);
        proc.add(output(new int[] {
                1, 100,
                2, 200,
        }));
        proc.add(output(new int[] {
                2, 201,
                3, 300,
        }));
        proc.add(output(new int[] {
                3, 301,
                4, 400,
        }));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(inputs, hasSize(1));

        Map<Integer, List<Integer>> map = collect(inputs.get(0));
        assertThat(map.keySet(), containsInAnyOrder(1, 2, 3, 4));
        assertThat(map, hasEntry(equalTo(1), containsInAnyOrder(100)));
        assertThat(map, hasEntry(equalTo(2), containsInAnyOrder(200, 201)));
        assertThat(map, hasEntry(equalTo(3), containsInAnyOrder(300, 301)));
        assertThat(map, hasEntry(equalTo(4), containsInAnyOrder(400)));
    }

    /**
     * multiple records in the key.
     */
    @Test
    public void multiple_partitions() {
        ScatterGatherProcessor proc = new ScatterGatherProcessor(2, null);
        proc.add(output(new int[] {
                0,   0,
                1, 100,
                2, 200,
                3, 300,
                4, 400,
        }));
        proc.add(output(new int[] {
                5, 500,
                6, 600,
                7, 700,
                8, 800,
                9, 900,
        }));
        List<InputBufferCursor> inputs = proc.process();
        assertThat(inputs, hasSize(2));

        Map<Integer, List<Integer>> p0 = collect(inputs.get(0));
        Map<Integer, List<Integer>> p1 = collect(inputs.get(1));
        assertThat(p0.size() + p1.size(), is(10));
        assertThat("probably partitioned", p0.keySet(), is(not(empty())));
        assertThat("probably partitioned", p1.keySet(), is(not(empty())));

        Map<Integer, List<Integer>> map = new HashMap<>();
        map.putAll(p0);
        map.putAll(p1);
        assertThat(map.size(), is(10));

        assertThat(map.keySet(), containsInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        for (int i = 0; i <= 9; i++) {
            assertThat(map, hasEntry(equalTo(i), containsInAnyOrder(i * 100)));
        }
    }

    private OutputBufferFragment output(int... keyValuePairs) {
        List<Consumer<ByteBuffer>> consumers = new ArrayList<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            int key = keyValuePairs[i + 0];
            int value = keyValuePairs[i + 1];
            consumers.add(b -> b.putInt(key));
            consumers.add(b -> b.putInt(value));
        }
        @SuppressWarnings("unchecked")
        Consumer<ByteBuffer>[] cs = (Consumer<ByteBuffer>[]) consumers.toArray(new Consumer<?>[consumers.size()]);
        return BufferUtil.outputWithKeys(keyValuePairs.length * Integer.BYTES, cs);
    }

    private Map<Integer, List<Integer>> collect(InputBufferCursor input) {
        Map<Integer, List<Integer>> results = new HashMap<>();
        BufferUtil.collect(input, (k, v) -> {
            assertThat(k.remaining(), is(4));
            Integer key = k.getInt();
            assertThat(results, not(hasKey(key)));

            assertThat(v.remaining() % Integer.BYTES, is(0));
            List<Integer> values = new ArrayList<>();
            while (v.hasRemaining()) {
                values.add(v.getInt());
            }
            results.put(key, values);
        });
        return results;
    }
}
