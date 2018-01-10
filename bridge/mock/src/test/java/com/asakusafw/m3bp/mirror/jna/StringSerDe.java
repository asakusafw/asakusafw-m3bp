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
package com.asakusafw.m3bp.mirror.jna;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.asakusafw.dag.api.common.DataComparator;
import com.asakusafw.dag.api.common.KeyValueSerDe;
import com.asakusafw.dag.api.common.ValueSerDe;
import com.sun.jna.Pointer;

/**
 * ser/de {@code String} values.
 */
public class StringSerDe implements ValueSerDe, KeyValueSerDe, BufferComparator, DataComparator {

    @Override
    public void serialize(Object object, DataOutput output) throws IOException, InterruptedException {
        String value = (String) object;
        for (char c : value.toCharArray()) {
            output.writeChar(c);
        }
        output.writeChar(0);
    }

    @Override
    public Object deserialize(DataInput input) throws IOException, InterruptedException {
        StringBuilder buf = new StringBuilder();
        while (true) {
            char c = input.readChar();
            if (c == 0) {
                break;
            }
            buf.append(c);
        }
        return buf.toString();
    }

    @Override
    public void serializeKey(Object object, DataOutput output) throws IOException, InterruptedException {
        serialize(object, output);
    }

    @Override
    public void serializeValue(Object object, DataOutput output) throws IOException, InterruptedException {
        serialize(object, output);
    }

    @Override
    public Object deserializePair(DataInput keyInput, DataInput valueInput) throws IOException, InterruptedException {
        return deserialize(valueInput);
    }

    @Override
    public int compare(DataInput a, DataInput b) throws IOException {
        try {
            String aObj = (String) deserialize(a);
            String bObj = (String) deserialize(b);
            return aObj.compareTo(bObj);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean compare(Pointer a, Pointer b) {
        long offset = 0;
        while (true) {
            char ca = a.getChar(offset * 2);
            char cb = b.getChar(offset * 2);
            if (ca == 0) {
                if (cb == 0) {
                    return false;
                } else {
                    return true;
                }
            } else if (cb == 0) {
                return false;
            }
            offset++;
        }
    }
}
