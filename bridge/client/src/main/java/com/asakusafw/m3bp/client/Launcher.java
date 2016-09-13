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
package com.asakusafw.m3bp.client;

import static com.asakusafw.m3bp.client.Constants.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.bridge.launch.LaunchConfiguration;
import com.asakusafw.bridge.launch.LaunchConfigurationException;
import com.asakusafw.bridge.stage.StageInfo;
import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.api.processor.basic.BasicProcessorContext;
import com.asakusafw.dag.api.processor.extension.ProcessorContextExtension;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.InterruptibleIo;
import com.asakusafw.runtime.core.context.RuntimeContext;

/**
 * M3BP application entry.
 */
public class Launcher {

    static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    private static final Pattern SENSITIVE_KEY = Pattern.compile("pass", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String SENSITIVE_VALUE_MASK = "****"; //$NON-NLS-1$

    /**
     * Exit status: execution was successfully completed.
     */
    public static final int EXEC_SUCCESS = 0;

    /**
     * Exit status: execution was finished with error.
     */
    public static final int EXEC_ERROR = 1;

    /**
     * Exit status: execution was interrupted.
     */
    public static final int EXEC_INTERRUPTED = 2;

    private final LaunchConfiguration configuration;

    private final ClassLoader applicationLoader;

    /**
     * Creates a new instance.
     * @param configuration the launching configuration
     */
    public Launcher(LaunchConfiguration configuration) {
        this(Arguments.requireNonNull(configuration), configuration.getStageClient().getClassLoader());
    }

    /**
     * Creates a new instance.
     * @param configuration the launching configuration
     * @param classLoader the application class loader
     */
    public Launcher(LaunchConfiguration configuration, ClassLoader classLoader) {
        Arguments.requireNonNull(configuration);
        Arguments.requireNonNull(classLoader);
        this.configuration = configuration;
        this.applicationLoader = classLoader;
    }

    /**
     * Executes DAG.
     * @return the exit status
     * @see #EXEC_SUCCESS
     * @see #EXEC_ERROR
     * @see #EXEC_INTERRUPTED
     */
    public int exec() {
        BasicProcessorContext context = newContext();
        GraphInfo graph = GraphExecutor.extract(configuration.getStageClient());
        try (InterruptibleIo extension = applyExtensions(context)) {
            long start = System.currentTimeMillis();
            LOG.info(MessageFormat.format(
                    "DAG starting: {0}, vertices={1}",
                    configuration.getStageInfo(),
                    graph.getVertices().size()));
            GraphExecutor.execute(context, graph);
            long finish = System.currentTimeMillis();
            LOG.info(MessageFormat.format(
                    "DAG finished: {0}, vertices={1}, elapsed={2}ms",
                    configuration.getStageInfo(),
                    graph.getVertices().size(),
                    finish - start));
            return EXEC_SUCCESS;
        } catch (IOException e) {
            LOG.error(MessageFormat.format(
                    "DAG failed: {0}",
                    configuration.getStageInfo()), e);
            return EXEC_ERROR;
        } catch (InterruptedException e) {
            LOG.warn(MessageFormat.format(
                    "DAG interrupted: {0}",
                    configuration.getStageInfo()), e);
            return EXEC_INTERRUPTED;
        }
    }

    private BasicProcessorContext newContext() {
        BasicProcessorContext context = new BasicProcessorContext(applicationLoader);
        configuration.getEngineProperties().forEach((k, v) -> {
            if (k.startsWith(KEY_HADOOP_PREFIX) == false) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Engine configuration: {}={}", k, shadow(k, v));
                }
                context.withProperty(k, v);
            }
        });

        Configuration hadoop = new Configuration();
        configuration.getHadoopProperties().forEach((k, v) -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Hadoop configuration: {}={}", k, shadow(k, v));
            }
            hadoop.set(k, v);
        });
        configuration.getEngineProperties().forEach((k, v) -> {
            if (k.startsWith(KEY_HADOOP_PREFIX)) {
                String key = k.substring(KEY_HADOOP_PREFIX.length());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Hadoop configuration: {}={}", key, shadow(key, v));
                }
                hadoop.set(key, v);
            }
        });

        context.withResource(StageInfo.class, configuration.getStageInfo());
        context.withResource(Configuration.class, hadoop);
        return context;
    }

    private String shadow(String key, String value) {
        if (SENSITIVE_KEY.matcher(key).find()) {
            return SENSITIVE_VALUE_MASK;
        }
        return value;
    }

    private InterruptibleIo applyExtensions(BasicProcessorContext context) throws IOException, InterruptedException {
        ProcessorContextExtension extension = ProcessorContextExtension.load(context.getClassLoader());
        return extension.install(context, context.getEditor());
    }

    /**
     * Program entry.
     * @param args launching configurations
     * @throws LaunchConfigurationException if launching configuration is something wrong
     */
    public static void main(String... args) throws LaunchConfigurationException {
        int status = exec(Launcher.class.getClassLoader(), args);
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
        Launcher launcher = new Launcher(conf, loader);
        return launcher.exec();
    }
}
