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
package com.asakusafw.m3bp.mirror;

import java.io.DataOutput;
import java.io.IOException;

import com.asakusafw.lang.utils.buffer.DataIoUtils;

/**
 * Represents a page-based output buffer.
 */
public interface PageDataOutput extends DataOutput {

    /**
     * Tells the current writing key was finished.
     * @throws IOException if I/O error was occurred while writing
     */
    void endKey() throws IOException;

    /**
     * Tells the current writing page was finished.
     * @throws IOException if I/O error was occurred while writing
     */
    void endPage() throws IOException;

    @Override
    default void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    default void writeBoolean(boolean v) throws IOException {
        writeByte(v ? 1 : 0);
    }

    @Override
    default void writeBytes(String s) throws IOException {
        for (int i = 0, n = s.length(); i < n; i++) {
            writeByte(s.charAt(i));
        }
    }

    @Override
    default void writeChars(String s) throws IOException {
        for (int i = 0, n = s.length(); i < n; i++) {
            writeChar(s.charAt(i));
        }
    }

    @Override
    default void writeUTF(String s) throws IOException {
        DataIoUtils.writeUTF(this, s);
    }
}
