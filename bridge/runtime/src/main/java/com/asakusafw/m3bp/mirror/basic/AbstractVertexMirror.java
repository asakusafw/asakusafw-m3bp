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
package com.asakusafw.m3bp.mirror.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.PortMirror;
import com.asakusafw.m3bp.mirror.VertexMirror;

/**
 * An abstract implementation of {@link VertexMirror}.
 */
public abstract class AbstractVertexMirror implements VertexMirror {

    private final List<PortMirror> inputs = new ArrayList<>();

    private final List<PortMirror> outputs = new ArrayList<>();

    /**
     * Returns the descriptor.
     * @return the descriptor
     */
    protected abstract M3bpVertexDescriptor getDescriptor();

    @Override
    public List<PortMirror> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    @Override
    public List<PortMirror> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    @Override
    public VertexProcessor newProcessor(ClassLoader loader) {
        SupplierInfo supplier = getDescriptor().getVertexProcessor();
        return (VertexProcessor) supplier.newInstance(loader).get();
    }

    @Override
    public PortMirror addInput(String name, M3bpEdgeDescriptor descriptor) {
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(descriptor);
        Identifier id = new Identifier(inputs.size());
        PortMirror port = createInput(id, name, descriptor);
        inputs.add(port);
        return port;
    }

    @Override
    public PortMirror addOutput(String name, M3bpEdgeDescriptor descriptor) {
        Arguments.requireNonNull(name);
        Arguments.requireNonNull(descriptor);
        Identifier id = new Identifier(outputs.size());
        PortMirror port = createOutput(id, name, descriptor);
        outputs.add(port);
        return port;
    }

    /**
     * Creates a new input port.
     * @param id the port ID
     * @param name the port name
     * @param descriptor the edge descriptor
     * @return the created port
     */
    protected abstract PortMirror createInput(Identifier id, String name, M3bpEdgeDescriptor descriptor);

    /**
     * Creates a new output port.
     * @param id the port ID
     * @param name the port name
     * @param descriptor the edge descriptor
     * @return the created port
     */
    protected abstract PortMirror createOutput(Identifier id, String name, M3bpEdgeDescriptor descriptor);
}
