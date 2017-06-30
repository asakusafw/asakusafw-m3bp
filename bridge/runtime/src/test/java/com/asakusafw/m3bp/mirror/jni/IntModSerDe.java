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
package com.asakusafw.m3bp.mirror.jni;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.asakusafw.dag.api.common.DataComparator;
import com.asakusafw.dag.api.common.KeyValueSerDe;

/**
 * {@link KeyValueSerDe}.
 */
public class IntModSerDe implements KeyValueSerDe, DataComparator {

    private final int modulo = 10;

    @Override
    public void serializeKey(Object object, DataOutput output) throws IOException, InterruptedException {
        output.writeInt((Integer) object % modulo);
    }

    @Override
    public void serializeValue(Object object, DataOutput output) throws IOException, InterruptedException {
        output.writeInt((Integer) object);
    }

    @Override
    public Object deserializePair(DataInput keyInput, DataInput valueInput) throws IOException, InterruptedException {
        keyInput.skipBytes(Integer.BYTES);
        return valueInput.readInt();
    }

    @Override
    public int compare(DataInput a, DataInput b) throws IOException {
        int vA = a.readInt();
        int vB = b.readInt();
        return Integer.compare(vA, vB);
    }
}
