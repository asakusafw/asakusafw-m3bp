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
package com.asakusafw.m3bp.compiler.windgate;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.asakusafw.dag.compiler.jdbc.windgate.WindGateJdbcIoAnalyzer;
import com.asakusafw.dag.runtime.jdbc.operation.JdbcEnvironmentInstaller;
import com.asakusafw.dag.runtime.testing.MockDataModel;
import com.asakusafw.lang.compiler.packaging.ResourceUtil;
import com.asakusafw.lang.compiler.tester.CompilerProfile;
import com.asakusafw.lang.compiler.tester.executor.JobflowExecutor;
import com.asakusafw.m3bp.client.Capability;
import com.asakusafw.m3bp.client.Constants;
import com.asakusafw.m3bp.compiler.common.CommandPath;
import com.asakusafw.m3bp.compiler.common.M3bpTask;
import com.asakusafw.m3bp.compiler.core.M3bpCompilerTesterRoot;
import com.asakusafw.m3bp.compiler.core.extension.NativeValueComparatorParticipant;
import com.asakusafw.m3bp.compiler.tester.InProcessM3bpTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestInput;
import com.asakusafw.m3bp.compiler.tester.externalio.TestIoTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestOutput;
import com.asakusafw.m3bp.compiler.windgate.testing.JdbcInput;
import com.asakusafw.m3bp.compiler.windgate.testing.JdbcOutput;
import com.asakusafw.m3bp.compiler.windgate.testing.JdbcTestHelper;
import com.asakusafw.m3bp.compiler.windgate.testing.MockJdbcSupport;
import com.asakusafw.runtime.windows.WindowsSupport;

/**
 * Test for {@link WindGateJdbcPortDriver}.
 */
public class WindGateJdbcPortDriverTest extends M3bpCompilerTesterRoot {

    static final File WORKING = new File("target/" + WindGateJdbcPortDriverTest.class.getSimpleName());

    /**
     * Support for Windows platform.
     */
    @ClassRule
    public static final WindowsSupport WINDOWS_SUPPORT = new WindowsSupport();

    /**
     * profile helper.
     */
    @Rule
    public final ExternalResource initializer = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            profile.forToolRepository()
                .useDefaults();
            profile.forFrameworkInstallation()
                .add(M3bpTask.PATH_ENGINE_CONFIG, o -> {
                    Properties p = new Properties();
                    p.putAll(engineConfig);
                    p.putIfAbsent(Constants.KEY_ENGINE_MOCK, Capability.POSSIBLE.name());
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

    final Map<String, String> engineConfig = new LinkedHashMap<>();

    /**
     * RDB for testing.
     */
    @Rule
    public final JdbcTestHelper helper = new JdbcTestHelper("testing");

    final TestIoTaskExecutor testio = new TestIoTaskExecutor();

    final JobflowExecutor executor = new JobflowExecutor(Arrays.asList(new InProcessM3bpTaskExecutor(), testio))
            .withBefore(testio::check)
            .withBefore((a, c) -> ResourceUtil.delete(WORKING))
            .withAfter((a, c) -> ResourceUtil.delete(WORKING));

    /**
     * input.
     * @throws Exception if failed
     */
    @Test
    public void input() throws Exception {
        helper.execute(MockJdbcSupport.ddl("a"));
        helper.insert(MockJdbcSupport.insert("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        activate("testing");
        run(profile, executor, g -> g
                .input("in", jdbcIn("testing", "a"))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * output.
     * @throws Exception if failed
     */
    @Test
    public void output() throws Exception {
        helper.execute(MockJdbcSupport.ddl("a"));
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        activate("testing");
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", jdbcOut("testing", "a"))
                .connect("in", "out"));
        helper.select(MockJdbcSupport.select("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    /**
     * I/O barrier.
     * @throws Exception if failed
     */
    @Test
    public void barrier() throws Exception {
        helper.execute(MockJdbcSupport.ddl("a"));
        helper.execute(MockJdbcSupport.ddl("b"));
        helper.insert(MockJdbcSupport.insert("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        activate("testing");
        run(profile, executor, g -> g
                .input("in", jdbcIn("testing", "a"))
                .output("out", jdbcOut("testing", "b"))
                .connect("in", "out"));
        helper.select(MockJdbcSupport.select("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    /**
     * w/o I/O barrier.
     * @throws Exception if failed
     */
    @Test
    public void barrier_disabled() throws Exception {
        profile.forCompilerOptions()
            .withProperty(WindGateJdbcPortDriver.KEY_BARRIER, String.valueOf(false));
        helper.execute(MockJdbcSupport.ddl("a"));
        helper.execute(MockJdbcSupport.ddl("b"));
        helper.insert(MockJdbcSupport.insert("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        activate("testing");
        run(profile, executor, g -> g
                .input("in", jdbcIn("testing", "a"))
                .output("out", jdbcOut("testing", "b"))
                .connect("in", "out"));
        helper.select(MockJdbcSupport.select("b"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    /**
     * multiple output ports.
     * @throws Exception if failed
     */
    @Test
    public void output_multiple() throws Exception {
        helper.execute(MockJdbcSupport.ddl("a"));
        helper.execute(MockJdbcSupport.ddl("b"));
        helper.execute(MockJdbcSupport.ddl("c"));
        testio.input("t0", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
        });
        testio.input("t1", MockDataModel.class, o -> {
            o.write(new MockDataModel(1, "Hello1"));
        });
        testio.input("t2", MockDataModel.class, o -> {
            o.write(new MockDataModel(2, "Hello2"));
        });
        activate("testing");
        run(profile, executor, g -> g
                .input("i0", TestInput.of("t0", MockDataModel.class))
                .input("i1", TestInput.of("t1", MockDataModel.class))
                .input("i2", TestInput.of("t2", MockDataModel.class))
                .output("o0", jdbcOut("testing", "a")).connect("i0", "o0")
                .output("o1", jdbcOut("testing", "b")).connect("i1", "o1")
                .output("o2", jdbcOut("testing", "c")).connect("i2", "o2"));
        helper.select(MockJdbcSupport.select("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello0")));
        });
        helper.select(MockJdbcSupport.select("b"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(1, "Hello1")));
        });
        helper.select(MockJdbcSupport.select("c"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(2, "Hello2")));
        });
    }

    /**
     * I/O barrier.
     * @throws Exception if failed
     */
    @Test
    public void barrier_multiple() throws Exception {
        helper.execute(MockJdbcSupport.ddl("a"));
        helper.execute(MockJdbcSupport.ddl("b"));
        helper.execute(MockJdbcSupport.ddl("c"));
        helper.execute(MockJdbcSupport.ddl("d"));
        helper.insert(MockJdbcSupport.insert("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        activate("testing");
        run(profile, executor, g -> g
                .input("in", jdbcIn("testing", "a"))
                .output("o0", jdbcOut("testing", "b")).connect("in", "o0")
                .output("o1", jdbcOut("testing", "c")).connect("in", "o1")
                .output("o2", jdbcOut("testing", "d")).connect("in", "o2"));
        helper.select(MockJdbcSupport.select("b"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        helper.select(MockJdbcSupport.select("c"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        helper.select(MockJdbcSupport.select("d"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    /**
     * orphaned output.
     * @throws Exception if failed
     */
    @Test
    public void output_orphaned() throws Exception {
        helper.execute(MockJdbcSupport.ddl("a"));
        helper.insert(MockJdbcSupport.insert("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            o.write(new MockDataModel(0, "ERROR"));
        });
        activate("testing");
        run(profile, executor, g -> g
                .output("out", jdbcOut("testing", "a")));
        helper.select(MockJdbcSupport.select("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, hasSize(0));
        });
    }

    /**
     * orphaned output.
     * @throws Exception if failed
     */
    @Test
    public void output_mixed_orphaned() throws Exception {
        helper.execute(MockJdbcSupport.ddl("a"));
        helper.execute(MockJdbcSupport.ddl("b"));
        helper.insert(MockJdbcSupport.insert("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            o.write(new MockDataModel(0, "ERROR"));
        });
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        activate("testing");
        run(profile, executor, g -> g
                .output("orphaned", jdbcOut("testing", "a"))
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", jdbcOut("testing", "b")).connect("in", "out"));
        helper.select(MockJdbcSupport.select("a"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, hasSize(0));
        });
        helper.select(MockJdbcSupport.select("b"), MockJdbcSupport.COLUMNS, MockJdbcSupport.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    private void activate(String... profiles) {
        profile.forCompilerOptions()
            .withProperty(WindGateJdbcIoAnalyzer.KEY_DIRECT, String.join(",", profiles));
        for (String name : profiles) {
            engineConfig.put(
                    String.format("%s%s.%s",
                            JdbcEnvironmentInstaller.KEY_PREFIX,
                            name,
                            JdbcEnvironmentInstaller.KEY_URL),
                    helper.getJdbcUrl());
        }
    }

    private JdbcInput jdbcIn(String profileName, String tableName) {
        return new JdbcInput(
                MockDataModel.class,
                profileName,
                tableName,
                MockJdbcSupport.COLUMNS,
                MockJdbcSupport.class);
    }

    private JdbcOutput jdbcOut(String profileName, String tableName) {
        return new JdbcOutput(
                MockDataModel.class,
                profileName,
                tableName,
                MockJdbcSupport.COLUMNS,
                MockJdbcSupport.class);
    }
}
