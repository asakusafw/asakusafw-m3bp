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

import java.io.IOException;

import com.asakusafw.dag.api.common.KeyValueDeserializer;
import com.asakusafw.dag.api.processor.GroupReader;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.PageDataInput;

/**
 * M3BP bridge implementation of {@link GroupReader}.
 */
public class KeyValueReaderBridge implements GroupReader {

    private final InputReaderMirror reader;

    private final PageDataInput keys;

    private final PageDataInput values;

    private final KeyValueDeserializer deserializer;

    private final Group current;

    private Object next = null;

    /**
     * Creates a new instance.
     * @param reader the root reader
     * @param deserializer the value deserializer
     */
    public KeyValueReaderBridge(InputReaderMirror reader, KeyValueDeserializer deserializer) {
        Arguments.requireNonNull(reader);
        Arguments.requireNonNull(deserializer);
        this.reader = reader;
        this.keys = reader.getKeyInput();
        this.values = reader.getValueInput();
        this.deserializer = deserializer;
        this.current = new Group(keys, deserializer);
    }

    @Override
    public boolean nextGroup() throws IOException, InterruptedException {
        if (keys.next()) {
            if (values.next() == false) {
                throw new IllegalStateException();
            }
            next = null;
            return true;
        } else {
            next = null;
            return false;
        }
    }

    @Override
    public GroupInfo getGroup() throws IOException, InterruptedException {
        return current;
    }

    @Override
    public boolean nextObject() throws IOException, InterruptedException {
        if (values.hasRemaining()) {
            next = deserializer.deserializePair(keys, values);
            keys.rewind();
            return true;
        } else {
            next = null;
            return false;
        }
    }

    @Override
    public Object getObject() throws IOException, InterruptedException {
        assert next != null;
        return next;
    }

    @Override
    public void close() throws IOException, InterruptedException {
        reader.close();
    }

    private static final class Group implements GroupInfo {

        private final PageDataInput input;

        private final KeyValueDeserializer deser;

        Group(PageDataInput input, KeyValueDeserializer deser) {
            this.input = input;
            this.deser = deser;
        }

        @Override
        public Object getValue() throws IOException, InterruptedException {
            Object result = deser.deserializeKey(input);
            input.rewind();
            return result;
        }

        @Override
        public int compareTo(GroupInfo o) {
            return input.comparePage(((Group) o).input);
        }
    }
}
