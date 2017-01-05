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
package com.asakusafw.m3bp.compiler.inspection.report;

import java.io.IOException;

import com.asakusafw.lang.utils.common.Io;
import com.asakusafw.lang.utils.common.RunnableWithException;
import com.asakusafw.m3bp.compiler.inspection.EdgeResolver;
import com.asakusafw.m3bp.compiler.inspection.VertexSpecView;

/**
 * Reports about {@link VertexSpecView}.
 */
public interface VertexReporter extends Io {

    /**
     * Reports about the graph.
     * @param vertex the target vertex
     * @param edges the edge resolver
     * @throws IOException if I/O error was occurred
     */
    void report(VertexSpecView vertex, EdgeResolver edges) throws IOException;

    /**
     * Reports a section in {@link VertexSpecView}.
     */
    @FunctionalInterface
    public interface SectionReporter {

        /**
         * Reports about a section.
         * @param writer the target writer
         * @param vertex the target vertex
         * @param edges the edge resolver
         * @throws IOException if I/O error was occurred
         */
        void report(ReportWriter writer, VertexSpecView vertex, EdgeResolver edges) throws IOException;
    }

    /**
     * Writes report contents.
     */
    public interface ReportWriter {

        /**
         * Enters into a new block.
         * @return this
         * @throws IOException if I/O error was occurred
         */
        ReportWriter enter() throws IOException;

        /**
         * Exits from the current block.
         * @return this
         * @throws IOException if I/O error was occurred
         */
        ReportWriter exit() throws IOException;

        /**
         * Writes in block.
         * @param line the first line
         * @param action the block action
         * @return this
         * @throws IOException if I/O error was occurred
         */
        default ReportWriter block(String line, RunnableWithException<IOException> action) throws IOException {
            append(line);
            enter();
            action.run();
            exit();
            return this;
        }

        /**
         * Writes in block.
         * @param action the block action
         * @return this
         * @throws IOException if I/O error was occurred
         */
        default ReportWriter block(RunnableWithException<IOException> action) throws IOException {
            enter();
            action.run();
            exit();
            return this;
        }

        /**
         * Appends a line.
         * @param line the line string
         * @return this
         * @throws IOException if I/O error was occurred
         */
        ReportWriter append(String line) throws IOException;
    }
}
