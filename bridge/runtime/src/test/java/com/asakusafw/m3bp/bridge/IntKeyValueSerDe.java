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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.asakusafw.dag.api.common.KeyValueSerDe;

/**
 * Serializes Integer as Key-Value pair.
 */
public class IntKeyValueSerDe implements KeyValueSerDe {

    private final int keyDelta;

    private final int valueDelta;

    /**
     * Creates a new instance.
     */
    public IntKeyValueSerDe() {
        this(0, 0);
    }

    /**
     * Creates a new instance.
     * @param keyDelta the key delta
     * @param valueDelta the value delta
     */
    public IntKeyValueSerDe(int keyDelta, int valueDelta) {
        this.keyDelta = keyDelta;
        this.valueDelta = valueDelta;
    }

    @Override
    public void serializeKey(Object object, DataOutput output) throws IOException, InterruptedException {
        output.writeInt((Integer) object + keyDelta);
    }

    @Override
    public void serializeValue(Object object, DataOutput output) throws IOException, InterruptedException {
        output.writeInt((Integer) object + valueDelta);
    }

    @Override
    public Object deserializeKey(DataInput keyInput) throws IOException, InterruptedException {
        return keyInput.readInt() - keyDelta;
    }

    @Override
    public Object deserializePair(DataInput keyInput, DataInput valueInput) throws IOException, InterruptedException {
        return valueInput.readInt() - valueDelta;
    }
}
