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
package com.asakusafw.m3bp.mirror.basic;

import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.TaskMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * An abstract implementation of {@link TaskMirror}.
 */
public abstract class AbstractTaskMirror implements TaskMirror {

    /**
     * Returns the task target vertex.
     * @return the vertex
     */
    protected abstract VertexMirror getVertex();

    /**
     * Returns an {@link InputReaderMirror} for the input port.
     * @param port the target port
     * @return corresponded mirror
     */
    protected abstract InputReaderMirror input(PortMirror port);

    /**
     * Returns an {@link OutputWriterMirror} for the input port.
     * @param port the target port
     * @return corresponded mirror
     */
    protected abstract OutputWriterMirror output(PortMirror port);

    @Override
    public InputReaderMirror input(Identifier id) {
        Arguments.requireNonNull(id);
        return input(port(getVertex().getInputs(), id));
    }

    @Override
    public OutputWriterMirror output(Identifier id) {
        Arguments.requireNonNull(id);
        return output(port(getVertex().getOutputs(), id));
    }

    private PortMirror port(List<? extends PortMirror> ports, Identifier id) {
        return ports.stream()
            .filter(p -> p.getId().equals(id))
            .findAny()
            .orElseThrow(() -> new NoSuchElementException(MessageFormat.format(
                    "unknown port ID: {1} ({0})", //$NON-NLS-1$
                    getVertex().getName(),
                    id)));
    }
}
