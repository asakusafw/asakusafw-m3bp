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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.asakusafw.dag.utils.common.Optionals;

/**
 * Provides edge I/O objects.
 */
public class EdgeIoProvider {

    private final Map<String, InputReaderMirror> inputs = new HashMap<>();

    private final Map<String, OutputWriterMirror> outputs = new HashMap<>();

    /**
     * Adds an input reader.
     * @param vertexName the vertex name
     * @param portName the port name
     * @param mirror the reader
     * @return this
     */
    public EdgeIoProvider add(String vertexName, String portName, InputReaderMirror mirror) {
        String id = id(vertexName, portName);
        inputs.put(id, mirror);
        return this;
    }

    /**
     * Adds an output writer.
     * @param vertexName the vertex name
     * @param portName the port name
     * @param mirror the writer
     * @return this
     */
    public EdgeIoProvider add(String vertexName, String portName, OutputWriterMirror mirror) {
        String id = id(vertexName, portName);
        outputs.put(id, mirror);
        return this;
    }

    /**
     * Returns an input reader.
     * @param vertexName the vertex name
     * @param portName the port name
     * @return the reader
     */
    public InputReaderMirror getInput(String vertexName, String portName) {
        String id = id(vertexName, portName);
        return Optionals.get(inputs, id).orElseThrow(() -> new NoSuchElementException(id));
    }

    /**
     * Returns an output writer.
     * @param vertexName the vertex name
     * @param portName the port name
     * @return the writer
     */
    public OutputWriterMirror getOutput(String vertexName, String portName) {
        String id = id(vertexName, portName);
        return Optionals.get(outputs, id).orElseThrow(() -> new NoSuchElementException(id));
    }

    private String id(String vertexName, String portName) {
        return String.format("%s:%s", vertexName, portName);
    }
}
