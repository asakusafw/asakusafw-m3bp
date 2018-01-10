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
package com.asakusafw.m3bp.bridge;

import java.io.IOException;

import com.asakusafw.dag.api.common.Serializer;
import com.asakusafw.dag.api.processor.ObjectWriter;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.PageDataOutput;

/**
 * M3BP bridge implementation of {@link ObjectWriter} for value only outputs.
 */
public class ValueWriterBridge implements ObjectWriter {

    private final OutputWriterMirror writer;

    private final Serializer serializer;

    /**
     * Creates a new instance.
     * @param writer the root writer
     * @param serializer the serializer
     */
    public ValueWriterBridge(OutputWriterMirror writer, Serializer serializer) {
        Arguments.requireNonNull(writer);
        Arguments.requireNonNull(serializer);
        this.writer = writer;
        this.serializer = serializer;
    }

    @Override
    public void putObject(Object object) throws IOException, InterruptedException {
        PageDataOutput o = writer.getOutput();
        serializer.serialize(object, o);
        o.endPage();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        writer.close();
    }
}
