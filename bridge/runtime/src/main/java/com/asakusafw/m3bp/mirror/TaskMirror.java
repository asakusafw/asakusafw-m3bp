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

/**
 * A mirror of M3BP {@code Task}.
 */
public interface TaskMirror {

    /**
     * Returns the logical task ID.
     * @return the logical task ID
     */
    Identifier logicalTaskId();

    /**
     * Returns the physical task ID.
     * @return the physical task ID
     */
    Identifier phisicalTaskId();

    /**
     * Returns {@link InputReaderMirror} for the target input port.
     * @param id the port ID
     * @return the corresponded reader
     */
    InputReaderMirror input(Identifier id);

    /**
     * Returns {@link OutputWriterMirror} for the target input port.
     * @param id the port ID
     * @return the corresponded writer
     */
    OutputWriterMirror output(Identifier id);

    /**
     * Returns whether the current task is cancelled or not.
     * @return {@code true} if this is cancelled, otherwise {@code false}
     */
    boolean isCancelled();
}
