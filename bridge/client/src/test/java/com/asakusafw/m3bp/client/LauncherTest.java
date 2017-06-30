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
package com.asakusafw.m3bp.client;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.asakusafw.bridge.launch.LaunchConfiguration;
import com.asakusafw.bridge.stage.StageInfo;
import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.api.processor.TaskProcessor;
import com.asakusafw.dag.api.processor.TaskSchedule;
import com.asakusafw.dag.api.processor.VertexProcessor;
import com.asakusafw.dag.api.processor.VertexProcessorContext;
import com.asakusafw.dag.api.processor.basic.BasicTaskInfo;
import com.asakusafw.dag.api.processor.basic.BasicTaskSchedule;
import com.asakusafw.lang.utils.common.Lang;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.m3bp.descriptor.Descriptors;

/**
 * Test for {@link M3bpLauncher}.
 */
public class LauncherTest {

    static final File NATIVE_DIR = new File("target/native/test/lib");

    static final String LIBRARY_NAME = "testing-" + M3bpLauncher.class.getSimpleName();

    /**
     * Detects native library file or skip tests.
     */
    @ClassRule
    public static final ExternalResource CHECKER = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            File f = new File(NATIVE_DIR, System.mapLibraryName(LIBRARY_NAME));
            Assume.assumeTrue(f.getPath(), f.isFile());
            nativeLibrary = f;
        }
    };

    /**
     * Cleaning each test.
     */
    @Rule
    public final ExternalResource CLEANER = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            clean0();
            properties.put(Constants.KEY_NATIVE_LIBRARY, nativeLibrary.getAbsolutePath());
            properties.put(Constants.KEY_ENGINE_MOCK, Capability.POSSIBLE.name());
        }
        @Override
        protected void after() {
            clean0();
        }
        private void clean0() {
            App.target = null;
        }
    };

    static File nativeLibrary;

    final Map<String, String> properties = new HashMap<>();

    final Map<String, String> hadoop = new HashMap<>();

    final Map<String, String> arguments = new HashMap<>();

    /**
     * simple case.
     */
    @Test
    public void simple() {
        GraphInfo graph = new GraphInfo();
        int exit = exec(graph);
        assertThat(exit, is(0));
    }

    /**
     * single vertex.
     */
    @Test
    public void single() {
        GraphInfo graph = new GraphInfo();
        graph.addVertex("v", Descriptors.newVertex(Single.class));

        try {
            Single.reset();
            Collections.addAll(Single.INPUT, "A", "B", "C");
            int exit = exec(graph);
            assertThat(exit, is(0));
            assertThat(Single.OUTPUT, containsInAnyOrder("A", "B", "C"));
        } finally {
            Single.reset();
        }
    }

    /**
     * stand-alone vertex.
     */
    public static class Single implements VertexProcessor {

        static List<Object> INPUT = Collections.synchronizedList(new ArrayList<>());

        static List<Object> OUTPUT = Collections.synchronizedList(new ArrayList<>());

        static void reset() {
            INPUT.clear();
            OUTPUT.clear();
        }

        @Override
        public Optional<? extends TaskSchedule> initialize(VertexProcessorContext context) {
            return Optionals.of(new BasicTaskSchedule(Lang.project(INPUT, BasicTaskInfo::new)));
        }

        @Override
        public TaskProcessor createTaskProcessor() {
            return context -> context.getTaskInfo()
                    .map(BasicTaskInfo.class::cast)
                    .map(BasicTaskInfo::getValue)
                    .ifPresent(OUTPUT::add);
        }
    }

    private int exec(GraphInfo graph) {
        App.target = graph;
        M3bpLauncher launcher = new M3bpLauncher(new LaunchConfiguration(
                App.class,
                new StageInfo("testing", "b", "f", null, "e", arguments),
                hadoop,
                properties));
        try {
            return launcher.exec();
        } finally {
            App.target = null;
        }
    }

    /**
     * Dummy application entry.
     */
    public static class App implements Supplier<GraphInfo> {

        static GraphInfo target;

        @Override
        public GraphInfo get() {
            assertThat(target, is(notNullValue()));
            return Lang.safe(() -> {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                GraphInfo.save(output, target);
                return GraphInfo.load(new ByteArrayInputStream(output.toByteArray()));
            });
        }
    }
}
