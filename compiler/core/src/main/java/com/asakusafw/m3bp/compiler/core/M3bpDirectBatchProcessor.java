/**
 * Copyright 2011-2021 Asakusa Framework Team.
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.compiler.flow.DirectLauncherScriptGenerator;
import com.asakusafw.lang.compiler.api.BatchProcessor;
import com.asakusafw.lang.compiler.api.reference.BatchReference;
import com.asakusafw.lang.compiler.api.reference.CommandTaskReference;
import com.asakusafw.lang.compiler.api.reference.JobflowReference;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.m3bp.compiler.common.M3bpTask;

/**
 * Generates launch scripts of direct Asakusa on M3BP jobflows.
 * @since 0.2.1
 */
public class M3bpDirectBatchProcessor implements BatchProcessor {

    static final Logger LOG = LoggerFactory.getLogger(M3bpDirectBatchProcessor.class);

    static final String KEY_ENABLED = M3bpJobflowProcessor.KEY_PREFIX + "launch.direct"; //$NON-NLS-1$

    static final boolean DEFAULT_ENABLED = false;

    private static final int ARG_APPLICATION_INDEX = 4;

    @Override
    public void process(Context context, BatchReference source) throws IOException {
        boolean enabled = context.getOptions().get(KEY_ENABLED, DEFAULT_ENABLED);
        LOG.debug("m3bp direct launching: {}", enabled); //$NON-NLS-1$
        if (enabled == false) {
            return;
        }
        for (JobflowReference jobflow : source.getJobflows()) {
            CommandTaskReference command = DirectLauncherScriptGenerator.findTask(jobflow)
                    .filter(task -> task.getCommand().equals(M3bpTask.PATH_COMMAND))
                    .filter(task -> task.getArguments().size() > ARG_APPLICATION_INDEX)
                    .orElse(null);
            if (command == null) {
                LOG.debug("{}:{} does not support m3bp direct launching", source.getBatchId(), jobflow.getFlowId()); //$NON-NLS-1$
                continue;
            }
            Location location = DirectLauncherScriptGenerator.getScriptLocation(jobflow.getFlowId());
            LOG.debug("generating direct launcher script: {}", location);
            try (Writer writer = new PrintWriter(
                    new OutputStreamWriter(context.addResourceFile(location), StandardCharsets.UTF_8))) {
                DirectLauncherScriptGenerator.write(
                        writer,
                        M3bpTask.PATH_COMMAND,
                        source.getBatchId(), jobflow.getFlowId(),
                        new ClassDescription(command.getArguments().get(ARG_APPLICATION_INDEX).getImage()));
            }
        }
    }
}
