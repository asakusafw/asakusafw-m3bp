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
package com.asakusafw.m3bp.compiler.directio;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.asakusafw.dag.runtime.testing.MockDataModel;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.extension.directio.DirectFileIoPortProcessor;
import com.asakusafw.lang.compiler.packaging.ResourceUtil;
import com.asakusafw.lang.compiler.tester.CompilerProfile;
import com.asakusafw.lang.compiler.tester.executor.JobflowExecutor;
import com.asakusafw.m3bp.client.Capability;
import com.asakusafw.m3bp.client.Constants;
import com.asakusafw.m3bp.compiler.common.CommandPath;
import com.asakusafw.m3bp.compiler.common.M3bpTask;
import com.asakusafw.m3bp.compiler.core.M3bpCompilerTesterRoot;
import com.asakusafw.m3bp.compiler.core.extension.NativeValueComparatorParticipant;
import com.asakusafw.m3bp.compiler.directio.testing.DirectFileInput;
import com.asakusafw.m3bp.compiler.directio.testing.DirectFileOutput;
import com.asakusafw.m3bp.compiler.directio.testing.DirectIoTestHelper;
import com.asakusafw.m3bp.compiler.directio.testing.MockDataFormat;
import com.asakusafw.m3bp.compiler.tester.InProcessM3bpTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestInput;
import com.asakusafw.m3bp.compiler.tester.externalio.TestIoTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestOutput;
import com.asakusafw.runtime.directio.DataFilter;
import com.asakusafw.runtime.windows.WindowsSupport;

/**
 * Test for {@link DirectFilePortDriver}.
 */
public class DirectFilePortDriverTest extends M3bpCompilerTesterRoot {

    static final File WORKING = new File("target/" + DirectFilePortDriverTest.class.getSimpleName());

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
    public final ExternalResource initializer = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            profile.forToolRepository()
                .useDefaults();
            profile.forFrameworkInstallation()
                .add(M3bpTask.PATH_ENGINE_CONFIG, o -> {
                    Properties p = new Properties();
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

    /**
     * Direct I/O helper.
     */
    @Rule
    public final DirectIoTestHelper helper = new DirectIoTestHelper();

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
        helper.input("input/a.bin", MockDataFormat.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", DirectFileInput.of("input", "*.bin", MockDataFormat.class))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * input w/ filter.
     * @throws Exception if failed
     */
    @Test
    public void input_filter() throws Exception {
        helper.input("input/a.bin", MockDataFormat.class, o -> {
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
                .input("in", DirectFileInput.of("input", "*.bin", MockDataFormat.class).withFilter(MockFilter.class))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * input w/ filter but the filter is disabled.
     * @throws Exception if failed
     */
    @Test
    public void input_filter_disabled() throws Exception {
        profile.forCompilerOptions()
            .withProperty(DirectFileIoPortProcessor.OPTION_FILTER_ENABLED, "false");
        helper.input("input/a.bin", MockDataFormat.class, o -> {
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
                .input("in", DirectFileInput.of("input", "*.bin", MockDataFormat.class).withFilter(MockFilter.class))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * flat output.
     * @throws Exception if failed
     */
    @Test
    public void output_flat() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", DirectFileOutput.of("output", "*.bin", MockDataFormat.class))
                .connect("in", "out"));
        helper.output("output", "*.bin", MockDataFormat.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    /**
     * group output.
     * @throws Exception if failed
     */
    @Test
    public void output_group() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello0"));
            o.write(new MockDataModel(1, "Hello1a"));
            o.write(new MockDataModel(1, "Hello1b"));
        });
        enableDirectIo();
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", DirectFileOutput.of("output", "{key}.bin", MockDataFormat.class).withOrder("-value"))
                .connect("in", "out"));
        helper.output("output", "0.bin", MockDataFormat.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello0")));
        });
        helper.output("output", "1.bin", MockDataFormat.class, o -> {
            assertThat(o, contains(new MockDataModel(1, "Hello1b"), new MockDataModel(1, "Hello1a")));
        });
    }

    /**
     * orphaned output.
     * @throws Exception if failed
     */
    @Test
    public void output_orphaned() throws Exception {
        enableDirectIo();
        run(profile, executor, g -> g
                .output("out", DirectFileOutput.of("output", "*.bin", MockDataFormat.class)
                        .withDeletePatterns("*.bin")));
        helper.output("output", "*.bin", MockDataFormat.class, o -> {
            assertThat(o, hasSize(0));
        });
    }

    private void enableDirectIo() {
        Configuration configuration = helper.getContext().newConfiguration();
        profile.forFrameworkInstallation().add(LOCATION_CORE_CONFIGURATION, o -> configuration.writeXml(o));
    }

    @SuppressWarnings("javadoc")
    public static class MockFilter extends DataFilter<MockDataModel> {

        @Override
        public boolean acceptsData(MockDataModel data) {
            return data.getKey() != 1;
        }
    }
}
