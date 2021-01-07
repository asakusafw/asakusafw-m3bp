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

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Lang;
import com.asakusafw.lang.utils.common.Optionals;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Scatter-Gather edge processor.
 */
public class ScatterGatherProcessor implements EdgeProcessor {

    static final int MAX_RECORD_COUNT = 1023;

    static final int BUFFER_SIZE_THREASHOLD = 250 * 1024;

    private final int partitions;

    private final Comparator<BufferEntry> comparator;

    private final List<OutputBufferFragment> upstreams = new ArrayList<>();

    /**
     * Creates a new instance.
     * @param partitions the number of partitions
     * @param comparator the value comparator
     */
    public ScatterGatherProcessor(int partitions, BufferComparator comparator) {
        Arguments.require(partitions > 0);
        this.partitions = partitions;
        this.comparator = Optionals.of(comparator)
                .<Comparator<BufferEntry>>map(ComparatorAdapter::new)
                .orElse(KeyComparator.INSTANCE);
    }

    @Override
    public void add(Iterable<? extends OutputBufferFragment> fragments) {
        Arguments.requireNonNull(fragments);
        fragments.forEach(upstreams::add);
    }

    @Override
    public List<InputBufferCursor> process() {
        List<List<BufferEntry>> entries = toEntries(upstreams);
        return Lang.let(new ArrayList<>(), it -> {
            for (List<BufferEntry> partition : entries) {
                it.add(toInputBuffer(partition));
                partition.clear();
            }
        });
    }

    private List<List<BufferEntry>> toEntries(Iterable<? extends OutputBufferFragment> fragments) {
        List<List<BufferEntry>> entries = Lang.let(new ArrayList<>(), it -> {
            Lang.repeat(partitions, () -> it.add(new ArrayList<>()));
        });
        Lang.forEach(fragments, fragment -> {
            for (long entryIndex = 0, n = fragment.getEntryCount(); entryIndex < n; entryIndex++) {
                BufferEntry entry = new BufferEntry(fragment, entryIndex);
                int partitionIndex = (entry.getKeyHash() & Integer.MAX_VALUE) % partitions;
                entries.get(partitionIndex).add(entry);
            }
        });
        Lang.forEach(entries, entry -> Collections.sort(entry, comparator));
        return entries;
    }

    private InputBufferCursor toInputBuffer(List<BufferEntry> entries) {
        return Lang.let(new InputBufferBuilder(), it -> Lang.forEach(entries, it::add)).build();
    }

    private static class BufferEntry {

        final OutputBufferFragment buffer;

        final long index;

        BufferEntry(OutputBufferFragment buffer, long index) {
            this.buffer = buffer;
            this.index = index;
        }

        int getKeyHash() {
            return getKey().hashCode();
        }

        ByteBuffer getKey() {
            long tableIndex = index * Long.BYTES;
            long offset = buffer.getEntryOffsets().getLong(tableIndex);
            long length = buffer.getKeyLengths().getLong(tableIndex);
            return buffer.getContents().getByteBuffer(offset, length);
        }

        ByteBuffer getValue() {
            long tableIndex = index * Long.BYTES;
            long offset = buffer.getEntryOffsets().getLong(tableIndex);
            long keySize = buffer.getKeyLengths().getLong(tableIndex);
            long nextOffset = buffer.getEntryOffsets().getLong(tableIndex + Long.BYTES);
            return buffer.getContents().getByteBuffer(offset + keySize, nextOffset - offset - keySize);
        }

        Pointer getValuePointer() {
            long tableIndex = index * Long.BYTES;
            long offset = buffer.getEntryOffsets().getLong(tableIndex) + buffer.getKeyLengths().getLong(tableIndex);
            return Util.add(buffer.getContents(), offset);
        }

        long getValueSize() {
            long tableIndex = index * Long.BYTES;
            Memory offsets = buffer.getEntryOffsets();
            return offsets.getLong(tableIndex + Long.BYTES) - offsets.getLong(tableIndex);
        }

        @Override
        public String toString() {
            return MessageFormat.format(
                    "BufferEntry(buffer={0}, index={1}, key={2}, value={3})",
                    buffer,
                    index,
                    getKey(),
                    getValue());
        }
    }

    private static class InputBufferBuilder {

        final LinkedList<InputBufferFragment> resultKeys = new LinkedList<>();

        final LinkedList<InputBufferFragment> resultValues = new LinkedList<>();

        final List<BufferEntry> entries = new ArrayList<>();

        final BitSet keyBreakIndices = new BitSet(MAX_RECORD_COUNT);

        ByteBuffer lastKey = null;

        int records = 0;

        int keySize = 0;

        int valueSize = 0;

        InputBufferBuilder() {
            return;
        }

        void add(BufferEntry entry) {
            ByteBuffer currentKey = entry.getKey();
            if (currentKey.equals(lastKey) == false) {
                if (records >= MAX_RECORD_COUNT
                        || keySize >= BUFFER_SIZE_THREASHOLD
                        || valueSize >= BUFFER_SIZE_THREASHOLD) {
                    flush();
                }
                lastKey = currentKey;
                keyBreakIndices.set(entries.size());
                records++;
                keySize += currentKey.remaining();
            }
            valueSize += (int) entry.getValueSize();
            entries.add(entry);
        }

        InputBufferCursor build() {
            if (entries.isEmpty() == false) {
                flush();
            }
            return new InputBufferCursor(resultKeys::poll, resultValues::poll);
        }

        private void flush() {
            resultKeys.add(buildKeys());
            resultValues.add(buildValues());
            entries.clear();
            keyBreakIndices.clear();
            lastKey = null;
            records = 0;
            keySize = 0;
            valueSize = 0;
        }

        private InputBufferFragment buildKeys() {
            Memory contents = new Memory(Math.max(keySize, 1));
            Memory offsets = new Memory((records + 1) * Long.BYTES);
            ByteBuffer contentsBuf = contents.getByteBuffer(0, contents.size());
            ByteBuffer offsetsBuf = offsets.getByteBuffer(0, offsets.size());
            offsetsBuf.putLong(0);

            List<BufferEntry> es = entries;
            BitSet bs = keyBreakIndices;
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                contentsBuf.put(es.get(i).getKey());
                offsetsBuf.putLong(contentsBuf.position());
            }
            return new InputBufferFragment(contents, offsets, records);
        }

        private InputBufferFragment buildValues() {
            Memory contents = new Memory(Math.max(valueSize, 1));
            Memory offsets = new Memory((records + 1) * Long.BYTES);
            ByteBuffer contentsBuf = contents.getByteBuffer(0, contents.size());
            ByteBuffer offsetsBuf = offsets.getByteBuffer(0, offsets.size());

            List<BufferEntry> es = entries;
            BitSet bs = keyBreakIndices;
            assert bs.get(0);
            for (int i = 0, n = es.size(); i < n; i++) {
                if (bs.get(i)) {
                    offsetsBuf.putLong(contentsBuf.position());
                }
                contentsBuf.put(es.get(i).getValue());
            }
            offsetsBuf.putLong(contentsBuf.position());
            return new InputBufferFragment(contents, offsets, records);
        }
    }

    private enum KeyComparator implements Comparator<BufferEntry> {

        INSTANCE,
        ;

        @Override
        public int compare(BufferEntry o1, BufferEntry o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    }

    private static class ComparatorAdapter implements Comparator<BufferEntry> {

        private final BufferComparator valueComparator;

        ComparatorAdapter(BufferComparator valueComparator) {
            this.valueComparator = valueComparator;
        }

        @Override
        public int compare(BufferEntry o1, BufferEntry o2) {
            int diff = o1.getKey().compareTo(o2.getKey());
            if (diff != 0) {
                return diff;
            }
            Pointer p1 = o1.getValuePointer();
            Pointer p2 = o2.getValuePointer();
            if (valueComparator.compare(p1, p2)) {
                return -1;
            } else if (valueComparator.compare(p2, p1)) {
                return +1;
            }
            return 0;
        }
    }
}
