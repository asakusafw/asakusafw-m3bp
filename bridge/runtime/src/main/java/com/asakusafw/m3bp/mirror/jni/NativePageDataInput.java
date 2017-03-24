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

import java.io.IOException;

import com.asakusafw.m3bp.mirror.PageDataInput;

class NativePageDataInput extends NativeDataInput implements PageDataInput {

    private final NativeDataInput offsets;

    private long currentBeginPosition;

    private long currentEndPosition;

    NativePageDataInput(long base, NativeDataInput entries) {
        super(base, computeLength(entries));
        this.offsets = entries;
        if (offsets.remaining() >= Long.BYTES) {
            this.currentBeginPosition = 0;
            this.currentEndPosition = offsets.readLong();
        } else {
            this.currentBeginPosition = 0;
            this.currentEndPosition = 0;
        }
    }

    private static long computeLength(NativeDataInput offsets) {
        if (offsets.remaining() >= Long.BYTES) {
            long start = offsets.position();
            offsets.position(offsets.limit() - Long.BYTES);
            long lastOffset = offsets.readLong();
            offsets.position(start);
            return lastOffset;
        }
        return 0L;
    }

    @Override
    public boolean next() throws IOException {
        if (offsets.hasRemaining() == false) {
            return false;
        }
        long begin = currentEndPosition;
        long end = offsets.readLong();
        region(begin, end - begin);
        currentBeginPosition = begin;
        currentEndPosition = end;
        return true;
    }

    @Override
    public void rewind() throws IOException {
        position(currentBeginPosition);
    }

    @Override
    public int comparePage(PageDataInput target) {
        return NativeDataInput.compareRegion(this, (NativeDataInput) target);
    }
}
