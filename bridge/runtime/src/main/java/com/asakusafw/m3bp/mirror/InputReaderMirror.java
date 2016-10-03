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
package com.asakusafw.m3bp.mirror;

import java.io.IOException;

import com.asakusafw.lang.utils.common.InterruptibleIo;

/**
 * A mirror of M3BP {@code InputReader}.
 */
public interface InputReaderMirror extends InterruptibleIo {

    /**
     * Returns the key input (optional operation).
     * @return the key input
     */
    PageDataInput getKeyInput();

    /**
     * Returns the value input.
     * @return the value input
     */
    PageDataInput getValueInput();

    @Override
    default void close() throws IOException, InterruptedException {
        return;
    }
}
