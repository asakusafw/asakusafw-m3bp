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
package com.asakusafw.m3bp.compiler.internalio;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.asakusafw.dag.runtime.testing.MockDataModel;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.packaging.ResourceUtil;
import com.asakusafw.lang.compiler.tester.CompilerProfile;
import com.asakusafw.lang.compiler.tester.executor.JobflowExecutor;
import com.asakusafw.m3bp.client.Capability;
import com.asakusafw.m3bp.client.Constants;
import com.asakusafw.m3bp.compiler.common.CommandPath;
import com.asakusafw.m3bp.compiler.common.M3bpTask;
import com.asakusafw.m3bp.compiler.core.M3bpCompilerTesterRoot;
import com.asakusafw.m3bp.compiler.core.extension.NativeValueComparatorParticipant;
import com.asakusafw.m3bp.compiler.internalio.testing.InternalIoTestHelper;
import com.asakusafw.m3bp.compiler.tester.InProcessM3bpTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestInput;
import com.asakusafw.m3bp.compiler.tester.externalio.TestIoTaskExecutor;
import com.asakusafw.m3bp.compiler.tester.externalio.TestOutput;
import com.asakusafw.runtime.windows.WindowsSupport;

/**
 * Test for {@link InternalPortDriver}.
 */
public class InternalPortDriverTest extends M3bpCompilerTesterRoot {

    static final File WORKING = new File("target/" + InternalPortDriverTest.class.getSimpleName());

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
     * Test helper.
     */
    @Rule
    public final InternalIoTestHelper helper = new InternalIoTestHelper();

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
        helper.input(MockDataModel.class, "input-*.bin", o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        run(profile, executor, g -> g
                .input("in", helper.input(MockDataModel.class, "input-*.bin"))
                .output("out", TestOutput.of("t", MockDataModel.class))
                .connect("in", "out"));
    }

    /**
     * output.
     * @throws Exception if failed
     */
    @Test
    public void output() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        run(profile, executor, g -> g
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", helper.output(MockDataModel.class, "output-*.bin"))
                .connect("in", "out"));
        helper.output(MockDataModel.class, "output-*.bin", o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
    }

    /**
     * orphaned output.
     * @throws Exception if failed
     */
    @Test
    public void output_orphaned() throws Exception {
        testio.input("t", MockDataModel.class, o -> {
            o.write(new MockDataModel(0, "Hello, world!"));
        });
        testio.output("t", MockDataModel.class, o -> {
            assertThat(o, contains(new MockDataModel(0, "Hello, world!")));
        });
        run(profile, executor, g -> g
                .output("orphaned", helper.output(MockDataModel.class, "output-*.bin"))
                .input("in", TestInput.of("t", MockDataModel.class))
                .output("out", TestOutput.of("t", MockDataModel.class)).connect("in", "out"));
        helper.output(MockDataModel.class, "output-*.bin", o -> {
            assertThat(o, hasSize(0));
        });
    }
}
