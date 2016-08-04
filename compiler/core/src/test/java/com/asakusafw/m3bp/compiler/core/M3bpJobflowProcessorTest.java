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
package com.asakusafw.m3bp.compiler.core;

import static com.asakusafw.lang.compiler.model.description.Descriptions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hadoop.conf.Configuration;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.asakusafw.dag.runtime.testing.MockDataModel;
import com.asakusafw.dag.runtime.testing.MockKeyValueModel;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.extension.directio.DirectFileIoPortProcessor;
import com.asakusafw.lang.compiler.model.graph.CoreOperator.CoreOperatorKind;
import com.asakusafw.lang.compiler.model.graph.Group;
import com.asakusafw.lang.compiler.model.graph.Groups;
import com.asakusafw.lang.compiler.model.graph.OperatorConstraint;
import com.asakusafw.lang.compiler.packaging.ResourceUtil;
import com.asakusafw.lang.compiler.tester.CompilerProfile;
import com.asakusafw.lang.compiler.tester.executor.JobflowExecutor;
import com.asakusafw.m3bp.client.Capability;
import com.asakusafw.m3bp.client.Constants;
import com.asakusafw.m3bp.compiler.common.M3bpTask;
import com.asakusafw.m3bp.compiler.core.extension.CommandPath;
import com.asakusafw.m3bp.compiler.core.extension.NativeValueComparatorParticipant;
import com.asakusafw.m3bp.compiler.core.testing.DirectInput;
import com.asakusafw.m3bp.compiler.core.testing.DirectIoTestHelper;
import com.asakusafw.m3bp.compiler.core.testing.DirectOutput;
import com.asakusafw.m3bp.compiler.core.testing.MockDataFormat;
import com.asakusafw.m3bp.compiler.tester.InProcessM3bpTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestInput;
import com.asakusafw.m3bp.compiler.tester.externalio.TestIoTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestOutput;
import com.asakusafw.runtime.core.Result;
import com.asakusafw.runtime.directio.DataFilter;
import com.asakusafw.runtime.windows.WindowsSupport;
import com.asakusafw.vocabulary.external.ImporterDescription.DataSize;
import com.asakusafw.vocabulary.flow.processor.PartialAggregation;
import com.asakusafw.vocabulary.model.Key;
import com.asakusafw.vocabulary.operator.CoGroup;
import com.asakusafw.vocabulary.operator.Convert;
import com.asakusafw.vocabulary.operator.Fold;
import com.asakusafw.vocabulary.operator.MasterJoinUpdate;
import com.asakusafw.vocabulary.operator.Update;

/**
 * Test for {@link M3bpJobflowProcessor}.
 */
public class M3bpJobflowProcessorTest extends M3bpCompilerTesterRoot {

    static final File WORKING = new File("target/" + M3bpJobflowProcessorTest.class.getSimpleName());

    static final Location LOCATION_CORE_CONFIGURATION = Location.of("core/conf/asakusa-resources.xml"); //$NON-NLS-1$

    /**
     * Support for Windows platform.
     */
    @ClassRule
    public static final WindowsSupport WINDOWS_SUPPORT = new WindowsSupport();

    /**
     * profile helper.
     */
    @Rule
    public final ExternalResource helper = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            profile.forToolRepository()
                .useDefaults();
            profile.forFrameworkInstallation()
                .add(M3bpTask.PATH_ENGINE_CONFIG, o -> {
                    Properties p = new Properties();
                    p.put(Constants.KEY_ENGINE_MOCK, Capability.POSSIBLE.name());
                    p.store(o, null);
                });
            profile.forCompilerOptions()
                .withRuntimeWorkingDirectory(WORKING.getAbsolutePath(), false);
            CommandPath path = CommandPath.system();
            if (isReady(path) == false) {
                path = path.append(new CommandPath(new File("/usr/local/bin")));
                Assume.assumeThat(isReady(path), is(true));
                profile.forCompilerOptions()
                    .withProperty(NativeValueComparatorParticipant.KEY_PATH, path.asPathString());
            }
        }
        private boolean isReady(CommandPath path) {
            return path.find(NativeValueComparatorParticipant.DEFAULT_CMAKE) != null
                    && path.find(NativeValueComparatorParticipant.DEFAULT_MAKE) != null;
        }
    };

    final CompilerProfile profile = new CompilerProfile(getClass().getClassLoader());

    /**
     * Direct I/O helper.
     */
    @Rule
    public final DirectIoTestHelper directio = new DirectIoTestHelper();

    final TestIoTaskExecutor testio = new TestIoTaskExecutor();

    final JobflowExecutor executor = new JobflowExecutor(Arrays.asList(new InProcessM3bpTaskExecutor(), testio))
            .withBefore(testio::check)
            .withBefore((a, c) -> ResourceUtil.delete(WORKING))
            .withAfter((a, c) -> ResourceUtil.delete(WORKING));

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * Direct I/O input.
     * @throws Exception if failed
     */
    @Test
    public void directio_input() throws Exception {
        directio.input("input/a.bin", MockDataFormat.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", DirectInput.of("input", "*.bin", MockDataFormat.class))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * Direct I/O input w/ filter.
     * @throws Exception if failed
     */
    @Test
    public void directio_input_filter() throws Exception {
        directio.input("input/a.bin", MockDataFormat.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1"));
            o.write(new MockDataModel(2, "Hello2"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(
                    new MockDataModel(0, "Hello0"),
                    new MockDataModel(2, "Hello2")));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", DirectInput.of("input", "*.bin", MockDataFormat.class).withFilter(MockFilter.class))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * Direct I/O input w/ filter but the filter is disabled.
     * @throws Exception if failed
     */
    @Test
    public void directio_input_filter_disabled() throws Exception {
        profile.forCompilerOptions()
            .withProperty(DirectFileIoPortProcessor.OPTION_FILTER_ENABLED, "false");
        directio.input("input/a.bin", MockDataFormat.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1"));
            o.write(new MockDataModel(2, "Hello2"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(
                    new MockDataModel(0, "Hello0"),
                    new MockDataModel(1, "Hello1"),
                    new MockDataModel(2, "Hello2")));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", DirectInput.of("input", "*.bin", MockDataFormat.class).withFilter(MockFilter.class))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * Direct I/O flat output.
     * @throws Exception if failed
     */
    @Test
    public void directio_output_flat() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", DirectOutput.of("output", "*.bin", MockDataFormat.class))
                .connect("in", "out"));
        directio.output("output", "*.bin", MockDataFormat.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    /**
     * Direct I/O group output.
     * @throws Exception if failed
     */
    @Test
    public void directio_output_group() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1a"));
            o.write(new MockDataModel(1, "Hello1b"));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", DirectOutput.of("output", "{key}.bin", MockDataFormat.class).withOrder("-value"))
                .connect("in", "out"));
        directio.output("output", "0.bin", MockDataFormat.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello0")));
        });
        directio.output("output", "1.bin", MockDataFormat.class, o -> {
            assertThat(o, contains(new MockDataModel(1, "Hello1b"), new MockDataModel(1, "Hello1a")));
        });
    }

    /**
     * Orphaned output.
     * @throws Exception if failed
     */
    @Test
    public void testio_output_orphaned() throws Exception {
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, hasSize(0));
        });
        run(profile, executor, g -> g
                .output("out", TestOutput.of("t", MockDataModel.class)
                        .withGenerator(true)));
    }

    /**
     * Direct I/O flat output.
     * @throws Exception if failed
     */
    @Test
    public void directio_output_orphaned() throws Exception {
        enableDirectIo();
        run(profile, executor, g -> g
                .output("out", DirectOutput.of("output", "*.bin", MockDataFormat.class)
                        .withDeletePatterns("*.bin")));
        directio.output("output", "*.bin", MockDataFormat.class, o -> {
            assertThat(o, hasSize(0));
        });
    }

    private void enableDirectIo() {
        Configuration configuration = directio.getContext().newConfiguration();
        profile.forFrameworkInstallation().add(LOCATION_CORE_CONFIGURATION, o -> configuration.writeXml(o));
    }

    /**
     * w/ extract kind operator.
     * @throws Exception if failed
     */
    @Test
    public void extract() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!?")));
        });
        /*
         * [In] -> [Extract] -> [Out]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("op", Ops.class, "update", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "op")
                .connect("op", "out"));
    }

    /**
     * w/ parameterized operator.
     * @throws Exception if failed
     */
    @Test
    public void arguments() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!$")));
        });
        /*
         * [In] -> [Extract] -> [Out]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("op", Ops.class, "parameterized", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .argument("suffix", valueOf("$"))
                        .build())
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "op")
                .connect("op", "out"));
    }

    /**
     * w/ checkpoint operator.
     * @throws Exception if failed
     */
    @Test
    public void checkpoint() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!?")));
        });
        /*
         * [In] -> [Checkpoint] -> [Extract] -> [Out]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("cp", CoreOperatorKind.CHECKPOINT, b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .operator("op", Ops.class, "update", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "cp")
                .connect("cp", "op")
                .connect("op", "out"));
    }

    /**
     * w/ co-group kind operator.
     * @throws Exception if failed
     */
    @Test
    public void cogroup() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, d(1), "Hello0"));
            o.write(new MockDataModel(0, d(0), "Hello1"));
            o.write(new MockDataModel(1, d(2), "Hello2"));
            o.write(new MockDataModel(1, d(0), "Hello3"));
            o.write(new MockDataModel(1, d(1), "Hello4"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, containsInAnyOrder(
                    new MockDataModel(0, d(0), "Hello1@0"),
                    new MockDataModel(0, d(1), "Hello0@1"),
                    new MockDataModel(1, d(0), "Hello3@0"),
                    new MockDataModel(1, d(1), "Hello4@1"),
                    new MockDataModel(1, d(2), "Hello2@2")));
        });
        /*
         * [In] -> [CoGroup] -> [Out]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("op", Ops.class, "group", b -> b
                        .input("in", typeOf(MockDataModel.class), group("key", "+sort"))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "op")
                .connect("op", "out"));
    }

    /**
     * w/ broadcast operator.
     * @throws Exception if failed
     */
    @Test
    public void broadcast() throws Exception {
        testio.input("in0", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1"));
            o.write(new MockDataModel(2, "Hello2"));
        });
        testio.input("in1", MockKeyValueModel.class, o -> {
            o.write(new MockKeyValueModel(0, "A"));
            o.write(new MockKeyValueModel(1, "B"));
        });
        testio.output("out0", MockDataModel.class, o -> {
            assertThat(o, containsInAnyOrder(
                    new MockDataModel(0, "Hello0@A"),
                    new MockDataModel(1, "Hello1@B")));
        });
        testio.output("out1", MockDataModel.class, o -> {
            assertThat(o, containsInAnyOrder(
                    new MockDataModel(2, "Hello2")));
        });
        /*
         * [In0] -> [Join] ----> [Out0]
         *            |   \
         * [In1] -----/    \--> [Out1]
         */
        run(profile, executor, g -> g
                .input("in0", TestInput.of("in0", MockDataModel.class))
                .input("in1", TestInput.of("in1", MockKeyValueModel.class, DataSize.TINY))
                .operator("op", Ops.class, "join", b -> b
                        .input("mst", typeOf(MockKeyValueModel.class), group("key"))
                        .input("tx", typeOf(MockDataModel.class), group("key"))
                        .output("joined", typeOf(MockDataModel.class))
                        .output("missed", typeOf(MockDataModel.class))
                        .build())
                .output("out0", TestOutput.of("out0", MockDataModel.class))
                .output("out1", TestOutput.of("out1", MockDataModel.class))
                .connect("in0", "op.tx")
                .connect("in1", "op.mst")
                .connect("op.joined", "out0")
                .connect("op.missed", "out1"));
    }
    /**
     * w/ aggregative operator.
     * @throws Exception if failed
     */
    @Test
    public void aggregate() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, d(1), ""));
            o.write(new MockDataModel(1, d(2), ""));
            o.write(new MockDataModel(1, d(3), ""));
            o.write(new MockDataModel(2, d(4), ""));
            o.write(new MockDataModel(2, d(5), ""));
            o.write(new MockDataModel(2, d(6), ""));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(
                    new MockDataModel(0, d(1), ""),
                    new MockDataModel(1, d(5), ""),
                    new MockDataModel(2, d(15), "")));
        });
        /*
         * [In] -> [Aggregate] -> [Out]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("op", Ops.class, "aggregate", b -> b
                        .input("in", typeOf(MockDataModel.class), group("key"))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "op")
                .connect("op", "out"));
    }

    /**
     * w/ aggregative operator.
     * @throws Exception if failed
     */
    @Test
    public void aggregate_pre() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, d(1), ""));
            o.write(new MockDataModel(1, d(2), ""));
            o.write(new MockDataModel(1, d(3), ""));
            o.write(new MockDataModel(2, d(4), ""));
            o.write(new MockDataModel(2, d(5), ""));
            o.write(new MockDataModel(2, d(6), ""));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(
                    new MockDataModel(0, d(1), ""),
                    new MockDataModel(1, d(5), ""),
                    new MockDataModel(2, d(15), "")));
        });
        /*
         * [In] -> [Aggregate] -> [Out]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("op", Ops.class, "preaggregate", b -> b
                        .input("in", typeOf(MockDataModel.class), group("key"))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "op")
                .connect("op", "out"));
    }

    /**
     * w/ buffer operator.
     * @throws Exception if failed
     */
    @Test
    public void buffer() throws Exception {
        testio.input("in", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello"));
        });
        testio.output("out0", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "HelloA")));
        });
        testio.output("out1", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "HelloB")));
        });
        testio.output("out2", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "HelloC")));
        });
        /*
         *          +-> [Extract0] -> [Out0]
         *         /
         * [In] --+---> [Extract1] -> [Out1]
         *         \
         *          +-> [Extract2] -> [Out2]
         *
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("in", MockDataModel.class))
                .operator("op0", Ops.class, "parameterized", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .argument("suffix", valueOf("A"))
                        .build())
                .operator("op1", Ops.class, "parameterized", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .argument("suffix", valueOf("B"))
                        .build())
                .operator("op2", Ops.class, "parameterized", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .argument("suffix", valueOf("C"))
                        .build())
                .output("out0", TestOutput.of("out0", MockDataModel.class))
                .output("out1", TestOutput.of("out1", MockDataModel.class))
                .output("out2", TestOutput.of("out2", MockDataModel.class))
                .connect("in", "op0", "op1", "op2")
                .connect("op0", "out0")
                .connect("op1", "out1")
                .connect("op2", "out2"));
    }

    /**
     * w/ sticky operator.
     * @throws Exception if failed
     */
    @Test
    public void sticky() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!?")));
        });
        /*
         * [In] --+--> [Extract] -> [Out]
         *         \
         *          -> [Sticky] -|
         */
        Ops.STICKY.set(false);
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("op", Ops.class, "update", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .operator("sticky", Ops.class, "sticky", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .constraint(OperatorConstraint.AT_LEAST_ONCE)
                        .build())
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "op", "sticky")
                .connect("op", "out"));
        assertThat(Ops.STICKY.get(), is(true));
    }

    /**
     * w/ sticky operator.
     * @throws Exception if failed
     */
    @Test
    public void sticky_without_other_edge_output() throws Exception {
        testio.input("in0", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.input("in1", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("out", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!?")));
        });
        /*
         * [In0] -> [Extract] -> [Out]
         * [In1] -> [Sticky] -|
         */
        Ops.STICKY.set(false);
        run(profile, executor, g -> g
                .input("in0", TestInput.of("in0", MockDataModel.class))
                .input("in1", TestInput.of("in1", MockDataModel.class))
                .operator("op", Ops.class, "update", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .build())
                .operator("sticky", Ops.class, "sticky", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("out", typeOf(MockDataModel.class))
                        .constraint(OperatorConstraint.AT_LEAST_ONCE)
                        .build())
                .output("out", TestOutput.of("out", MockDataModel.class))
                .connect("in0", "op")
                .connect("in1", "sticky")
                .connect("op", "out"));
        assertThat(Ops.STICKY.get(), is(true));
    }

    /**
     * operator w/ disconnected port.
     * @throws Exception if failed
     */
    @Test
    public void output_discard() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockKeyValueModel.class, o -> {
            assertThat(o, contains(new MockKeyValueModel(0, "Hello, world!")));
        });
        /*
         * [In] -> [Extract] --> [Out]
         *                  \
         *                   +-|
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .operator("op", Ops.class, "convert", b -> b
                        .input("in", typeOf(MockDataModel.class))
                        .output("original", typeOf(MockDataModel.class))
                        .output("converted", typeOf(MockKeyValueModel.class))
                        .build())
                .output("out", TestOutput.of("t", MockKeyValueModel.class))
                .connect("in", "op")
                .connect("op.converted", "out"));
    }

    /**
     * self join w/ scatter-gather.
     * @throws Exception if failed
     */
    @Test
    public void self_join_scatter_gather() throws Exception {
        testio.input("in", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1"));
            o.write(new MockDataModel(2, "Hello2"));
        });
        testio.output("out0", MockDataModel.class, o -> {
            assertThat(o, containsInAnyOrder(
                    new MockDataModel(0, "Hello0@Hello0"),
                    new MockDataModel(1, "Hello1@Hello1"),
                    new MockDataModel(2, "Hello2@Hello2")));
        });
        testio.output("out1", MockDataModel.class, o -> {
            assertThat(o, hasSize(0));
        });
        /*
         * [In] -+--> [Join] -> [Out0]
         *        \   |   \
         *         \--/    \--> [Out1]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("in", MockDataModel.class, DataSize.LARGE))
                .operator("op", Ops.class, "join_self", b -> b
                        .input("mst", typeOf(MockDataModel.class), group("key"))
                        .input("tx", typeOf(MockDataModel.class), group("key"))
                        .output("joined", typeOf(MockDataModel.class))
                        .output("missed", typeOf(MockDataModel.class))
                        .build())
                .output("out0", TestOutput.of("out0", MockDataModel.class))
                .output("out1", TestOutput.of("out1", MockDataModel.class))
                .connect("in", "op.tx", "op.mst")
                .connect("op.joined", "out0")
                .connect("op.missed", "out1"));
    }

    /**
     * self join w/ broadcast.
     * @throws Exception if failed
     */
    @Test
    public void self_join_broadcast() throws Exception {
        testio.input("in", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1"));
            o.write(new MockDataModel(2, "Hello2"));
        });
        testio.output("out0", MockDataModel.class, o -> {
            assertThat(o, containsInAnyOrder(
                    new MockDataModel(0, "Hello0@Hello0"),
                    new MockDataModel(1, "Hello1@Hello1"),
                    new MockDataModel(2, "Hello2@Hello2")));
        });
        testio.output("out1", MockDataModel.class, o -> {
            assertThat(o, hasSize(0));
        });
        /*
         * [In] -+--> [Join] -> [Out0]
         *        \   |   \
         *         \--/    \--> [Out1]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("in", MockDataModel.class, DataSize.TINY))
                .operator("op", Ops.class, "join_self", b -> b
                        .input("mst", typeOf(MockDataModel.class), group("key"))
                        .input("tx", typeOf(MockDataModel.class), group("key"))
                        .output("joined", typeOf(MockDataModel.class))
                        .output("missed", typeOf(MockDataModel.class))
                        .build())
                .output("out0", TestOutput.of("out0", MockDataModel.class))
                .output("out1", TestOutput.of("out1", MockDataModel.class))
                .connect("in", "op.tx", "op.mst")
                .connect("op.joined", "out0")
                .connect("op.missed", "out1"));
    }

    /**
     * join w/ orphaned master port.
     * @throws Exception if failed
     */
    @Test
    public void orphaned_master_join() throws Exception {
        testio.input("in", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1"));
            o.write(new MockDataModel(2, "Hello2"));
        });
        testio.output("out0", MockDataModel.class, o -> {
            assertThat(o, hasSize(0));
        });
        testio.output("out1", MockDataModel.class, o -> {
            assertThat(o, containsInAnyOrder(
                    new MockDataModel(0, "Hello0"),
                    new MockDataModel(1, "Hello1"),
                    new MockDataModel(2, "Hello2")));
        });
        /*
         * [In] ---> [Join] -> [Out0]
         *            |   \
         *        X|--/    \--> [Out1]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("in", MockDataModel.class))
                .operator("op", Ops.class, "join_self", b -> b
                        .input("mst", typeOf(MockDataModel.class), group("key"))
                        .input("tx", typeOf(MockDataModel.class), group("key"))
                        .output("joined", typeOf(MockDataModel.class))
                        .output("missed", typeOf(MockDataModel.class))
                        .build())
                .output("out0", TestOutput.of("out0", MockDataModel.class))
                .output("out1", TestOutput.of("out1", MockDataModel.class))
                .connect("in", "op.tx")
                .connect("op.joined", "out0")
                .connect("op.missed", "out1"));
    }

    /**
     * join w/ orphaned tx port.
     * @throws Exception if failed
     */
    @Test
    public void orphaned_tx_join() throws Exception {
        testio.input("in", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
        });
        testio.output("out0", MockDataModel.class, o -> {
            assertThat(o, hasSize(0));
        });
        testio.output("out1", MockDataModel.class, o -> {
            assertThat(o, hasSize(0));
        });
        /*
         *    X|-> [Join] -> [Out0]
         *         |   \
         * [In] ---/    \--> [Out1]
         */
        run(profile, executor, g -> g
                .input("in", TestInput.of("in", MockDataModel.class))
                .operator("op", Ops.class, "join_self", b -> b
                        .input("mst", typeOf(MockDataModel.class), group("key"))
                        .input("tx", typeOf(MockDataModel.class), group("key"))
                        .output("joined", typeOf(MockDataModel.class))
                        .output("missed", typeOf(MockDataModel.class))
                        .build())
                .output("out0", TestOutput.of("out0", MockDataModel.class))
                .output("out1", TestOutput.of("out1", MockDataModel.class))
                .connect("in", "op.mst")
                .connect("op.joined", "out0")
                .connect("op.missed", "out1"));
    }

    private static BigDecimal d(long value) {
        return new BigDecimal(value);
    }

    private static Group group(String... values) {
        List<String> group = new ArrayList<>();
        List<String> order = new ArrayList<>();
        for (String s : values) {
            char operator = s.charAt(0);
            switch (operator) {
            case '=':
                group.add(s.substring(1));
                break;
            case '+':
            case '-':
                order.add(s);
                break;
            default:
                group.add(s);
                break;
            }
        }
        return Groups.parse(group, order);
    }

    @SuppressWarnings("javadoc")
    public static class Ops {

        static final AtomicBoolean STICKY = new AtomicBoolean(false);

        @Update
        public void update(MockDataModel model) {
            model.setValue(model.getValue() + "?");
        }

        @Update
        public void parameterized(MockDataModel model, String suffix) {
            model.setValue(model.getValue() + suffix);
        }

        @CoGroup
        public void group(
                @Key(group = "key", order="sort") List<MockDataModel> in,
                Result<MockDataModel> out) {
            int index = 0;
            for (MockDataModel model : in) {
                model.setValue(model.getValue() + "@" + index++);
                out.add(model);
            }
        }

        @MasterJoinUpdate
        public void join(
                @Key(group = "key") MockKeyValueModel mst,
                @Key(group = "key") MockDataModel tx) {
            tx.setValue(tx.getValue() + "@" + mst.getValue());
        }

        @MasterJoinUpdate
        public void join_self(
                @Key(group = "key") MockDataModel mst,
                @Key(group = "key") MockDataModel tx) {
            tx.setValue(tx.getValue() + "@" + mst.getValue());
        }

        @Fold(partialAggregation = PartialAggregation.TOTAL)
        public void aggregate(@Key(group = "key") MockDataModel a, MockDataModel b) {
            a.setSort(a.getSort().add(b.getSort()));
        }

        @Fold(partialAggregation = PartialAggregation.PARTIAL)
        public void preaggregate(@Key(group = "key") MockDataModel a, MockDataModel b) {
            a.setSort(a.getSort().add(b.getSort()));
        }

        //@Sticky
        @Update
        public void sticky(MockDataModel model) {
            STICKY.set(true);
        }

        private final MockKeyValueModel kv = new MockKeyValueModel();

        @Convert
        public MockKeyValueModel convert(MockDataModel model) {
            kv.setKey(model.getKey());
            kv.setValue(model.getValue());
            return kv;
        }
    }

    @SuppressWarnings("javadoc")
    public static class MockFilter extends DataFilter<MockDataModel> {

        @Override
        public boolean acceptsData(MockDataModel data) {
            return data.getKey() != 1;
        }
    }
}
