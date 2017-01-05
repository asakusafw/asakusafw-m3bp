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
package com.asakusafw.m3bp.mirror.mock;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.ConfigurationMirror;
import com.asakusafw.m3bp.mirror.EngineMirror;
import com.asakusafw.m3bp.mirror.FlowGraphMirror;
import com.asakusafw.m3bp.mirror.basic.BasicConfigurationMirror;
import com.asakusafw.m3bp.mirror.basic.BasicFlowGraphMirror;
import com.asakusafw.m3bp.mirror.jna.BufferComparator;
import com.asakusafw.m3bp.mirror.jna.FlowGraphExecutor;
import com.asakusafw.m3bp.mirror.jna.NativeFunctionLoader;

/**
 * Mock implementation of {@link EngineMirror}.
 */
public class MockEngineMirror implements EngineMirror {

    static final Logger LOG = LoggerFactory.getLogger(MockEngineMirror.class);

    private final ConfigurationMirror configuration;

    private final FlowGraphMirror graph;

    private final NativeFunctionLoader library;

    /**
     * Creates a new instance.
     * @param library the library file
     */
    public MockEngineMirror(File library) {
        this.configuration = new BasicConfigurationMirror();
        this.graph = new BasicFlowGraphMirror();
        this.library = Arguments.map(library, NativeFunctionLoader::new);
    }

    @Override
    public ConfigurationMirror getConfiguration() {
        return configuration;
    }

    @Override
    public FlowGraphMirror getGraph() {
        return graph;
    }

    /**
     * Returns a comparator.
     * @param name the comparator name
     * @return the comparator
     * @throws NoSuchElementException if there is no such a comparator
     */
    public BufferComparator getComparator(String name) {
        Arguments.requireNonNull(name);
        if (library == null) {
            throw new NoSuchElementException(name);
        }
        try {
            return library.getComparator(name);
        } catch (UnsatisfiedLinkError e) {
            throw new NoSuchElementException(name);
        }
    }

    @Override
    public void run(ProcessorContext context) throws IOException, InterruptedException {
        Arguments.requireNonNull(context);
        new FlowGraphExecutor(
                context,
                graph, configuration,
                library == null ? null : library::getComparator).run();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        if (library != null) {
            library.close();
        }
    }
}
