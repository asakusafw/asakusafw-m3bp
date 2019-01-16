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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.asakusafw.m3bp.mirror.PortMirror;

/**
 * Provides I/O for tasks.
 */
interface IoMap {

    /**
     * Returns what supplies empty {@link OutputBufferFragment}.
     * @param port the target port
     * @return supplier
     */
    Supplier<OutputBufferFragment> getOutputSource(PortMirror port);

    /**
     * Returns what receives flushed {@link OutputBufferFragment}.
     * @param port the target port
     * @return consumer
     */
    Consumer<OutputBufferFragment> getOutputSink(PortMirror port);

    /**
     * Returns the each input source for task.
     * @param port the target port
     * @return the input sources
     */
    List<InputBufferCursor> getInputSource(PortMirror port);
}
