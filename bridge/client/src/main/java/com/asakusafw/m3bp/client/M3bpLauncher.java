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
package com.asakusafw.m3bp.client;

import static com.asakusafw.m3bp.client.Constants.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.bridge.launch.LaunchConfiguration;
import com.asakusafw.bridge.launch.LaunchConfigurationException;
import com.asakusafw.bridge.launch.LaunchInfo;
import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.api.processor.basic.BasicProcessorContext;
import com.asakusafw.dag.api.processor.extension.ProcessorContextExtension;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.InterruptibleIo;
import com.asakusafw.runtime.core.context.RuntimeContext;
import com.asakusafw.vanilla.client.LaunchUtil;
import com.asakusafw.vanilla.client.VanillaLauncher;

/**
 * M3BP application entry.
 * @since 0.1.0
 * @version 0.2.2
 */
public class M3bpLauncher {

    static final Logger LOG = LoggerFactory.getLogger(M3bpLauncher.class);

    private final LaunchInfo configuration;

    private final ClassLoader applicationLoader;

    private final Configuration hadoop;

    /**
     * Creates a new instance.
     * @param configuration the launching configuration
     */
    public M3bpLauncher(LaunchInfo configuration) {
        this(Arguments.requireNonNull(configuration), configuration.getStageClient().getClassLoader());
    }

    /**
     * Creates a new instance.
     * @param configuration the launching configuration
     * @param classLoader the application class loader
     */
    public M3bpLauncher(LaunchInfo configuration, ClassLoader classLoader) {
        Arguments.requireNonNull(configuration);
        Arguments.requireNonNull(classLoader);
        this.configuration = configuration;
        this.applicationLoader = classLoader;
        this.hadoop = new Configuration();
        this.hadoop.setClassLoader(classLoader);
    }

    M3bpLauncher(LaunchInfo configuration, Configuration hadoop) {
        Arguments.requireNonNull(configuration);
        Arguments.requireNonNull(hadoop);
        this.configuration = configuration;
        this.hadoop = hadoop;
        this.applicationLoader = hadoop.getClassLoader();
    }

    /**
     * Executes DAG.
     * @return the exit status
     * @see LaunchUtil#EXEC_SUCCESS
     * @see LaunchUtil#EXEC_ERROR
     * @see LaunchUtil#EXEC_INTERRUPTED
     */
    public int exec() {
        BasicProcessorContext context =
                LaunchUtil.createProcessorContext(applicationLoader, configuration, hadoop);
        GraphInfo graph = LaunchUtil.extract(configuration.getStageClient());
        try (InterruptibleIo extension = applyExtensions(context)) {
            long start = System.currentTimeMillis();
            LOG.info(MessageFormat.format(
                    "DAG starting: {0}, vertices={1}",
                    configuration.getStageInfo(),
                    graph.getVertices().size()));
            if (isVanilla(context)) {
                LOG.info("using Vanilla engine");
                VanillaLauncher.execute(context, graph);
            } else {
                GraphExecutor.execute(context, graph);
            }
            long finish = System.currentTimeMillis();
            LOG.info(MessageFormat.format(
                    "DAG finished: {0}, vertices={1}, elapsed={2}ms",
                    configuration.getStageInfo(),
                    graph.getVertices().size(),
                    finish - start));
            return LaunchUtil.EXEC_SUCCESS;
        } catch (IOException e) {
            LOG.error(MessageFormat.format(
                    "DAG failed: {0}",
                    configuration.getStageInfo()), e);
            return LaunchUtil.EXEC_ERROR;
        } catch (InterruptedException e) {
            LOG.warn(MessageFormat.format(
                    "DAG interrupted: {0}",
                    configuration.getStageInfo()), e);
            return LaunchUtil.EXEC_INTERRUPTED;
        }
    }

    private static InterruptibleIo applyExtensions(
            BasicProcessorContext context) throws IOException, InterruptedException {
        ProcessorContextExtension extension = ProcessorContextExtension.load(context.getClassLoader());
        return extension.install(context, context.getEditor());
    }

    private static boolean isVanilla(ProcessorContext context) {
        return context.getProperty(KEY_ENGINE_VANILLA)
                .map(String::trim)
                .map(it -> Boolean.parseBoolean(it.trim()))
                .orElse(false);
    }

    /**
     * Program entry.
     * @param args launching configurations
     * @throws LaunchConfigurationException if launching configuration is something wrong
     */
    public static void main(String... args) throws LaunchConfigurationException {
        int status = exec(M3bpLauncher.class.getClassLoader(), args);
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * Program entry.
     * @param loader the launch class loader
     * @param args launching configurations
     * @return the exit code
     * @throws LaunchConfigurationException if launching configuration is something wrong
     */
    public static int exec(ClassLoader loader, String... args) throws LaunchConfigurationException {
        RuntimeContext.set(RuntimeContext.DEFAULT.apply(System.getenv()));
        RuntimeContext.get().verifyApplication(loader);

        LaunchConfiguration conf = LaunchConfiguration.parse(loader, Arrays.asList(args));
        M3bpLauncher launcher = new M3bpLauncher(conf, loader);
        return launcher.exec();
    }
}
