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
package com.asakusafw.m3bp.mirror;

import java.io.DataInput;
import java.io.IOException;

import com.asakusafw.lang.utils.buffer.DataIoUtils;

/**
 * {@link DataInput} with data scope.
 */
public interface PageDataInput extends DataInput {

    /**
     * Returns whether the next page exists or not.
     * @return {@code true} if the next page exists
     * @throws IOException if I/O error was occurred while reading the next page
     */
    boolean next() throws IOException;

    /**
     * Rewinds read cursor to the head of current page.
     * @throws IOException if I/O error was occurred while rewinding the cursor
     */
    void rewind() throws IOException;

    /**
     * Returns whether there is more remaining data in the current page or not.
     * @return {@code true} if there is more remaining data in the current page, otherwise {@code false}
     */
    boolean hasRemaining();

    /**
     * Compares the current page with the page on the target input.
     * @param target the target input
     * @return the comparison result
     */
    int comparePage(PageDataInput target);

    @Override
    default void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    default String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    default String readUTF() throws IOException {
        return DataIoUtils.readUTF(this);
    }
}
