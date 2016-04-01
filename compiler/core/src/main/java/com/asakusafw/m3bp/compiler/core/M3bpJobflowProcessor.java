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

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.compiler.codegen.ApplicationGenerator;
import com.asakusafw.dag.compiler.codegen.CleanupStageClientGenerator;
import com.asakusafw.dag.compiler.planner.DagPlanning;
import com.asakusafw.lang.compiler.api.Exclusive;
import com.asakusafw.lang.compiler.api.JobflowProcessor;
import com.asakusafw.lang.compiler.api.reference.CommandToken;
import com.asakusafw.lang.compiler.api.reference.TaskReference;
import com.asakusafw.lang.compiler.hadoop.HadoopCommandRequired;
import com.asakusafw.lang.compiler.inspection.InspectionExtension;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.graph.Jobflow;
import com.asakusafw.lang.compiler.planning.Plan;
import com.asakusafw.lang.compiler.planning.PlanDetail;
import com.asakusafw.m3bp.compiler.common.M3bpPackage;
import com.asakusafw.m3bp.compiler.common.M3bpTask;

/**
 * An implementation of {@link JobflowProcessor} for M3BP.
 */
@Exclusive
public class M3bpJobflowProcessor implements JobflowProcessor {

    static final Logger LOG = LoggerFactory.getLogger(M3bpDagGenerator.class);

    /**
     * The compiler option key prefix.
     */
    public static final String KEY_PREFIX = "m3bp."; //$NON-NLS-1$

    static final String KEY_CODEGEN = KEY_PREFIX + "codegen"; //$NON-NLS-1$

    @Override
    public void process(Context context, Jobflow source) throws IOException {
        LOG.debug("computing execution plan: {}", source.getFlowId());
        Plan plan = plan(context, source);
        try {
            if (context.getOptions().get(KEY_CODEGEN, true) == false) {
                LOG.info("code generation was skipped: {} ({}=true)", source.getFlowId(), KEY_CODEGEN);
                return;
            }
            M3bpCompilerContext c = new M3bpCompilerContext.Basic(context, source);
            LOG.debug("generating vertices: {}", source.getFlowId());
            GraphInfo graph = M3bpDagGenerator.generate(c, plan);
            LOG.debug("generating application entry: {}", source.getFlowId());
            addApplication(c, graph);
            LOG.debug("generating cleanup : {}", source.getFlowId());
            addCleanup(c);
        } finally {
            LOG.debug("generating inspection info: {} ({})",
                    source.getFlowId(), M3bpPackage.PATH_PLAN_INSPECTION);
            InspectionExtension.inspect(context, M3bpPackage.PATH_PLAN_INSPECTION, plan);
        }
    }

    private Plan plan(Context context, Jobflow source) {
        PlanDetail detail = DagPlanning.plan(context, source);
        return detail.getPlan();
    }

    private void addApplication(M3bpCompilerContext context, GraphInfo graph) {
        context.add(M3bpPackage.PATH_GRAPH_INFO, output -> GraphInfo.save(output, graph));
        ClassDescription application = context.add(new ApplicationGenerator().generate(
                M3bpPackage.PATH_GRAPH_INFO,
                new ClassDescription(M3bpPackage.CLASS_APPLICATION)));
        TaskReference task = context.getRoot().addTask(
                M3bpTask.MODULE_NAME,
                M3bpTask.PROFILE_NAME,
                M3bpTask.PATH_COMMAND,
                Arrays.asList(new CommandToken[] {
                        CommandToken.BATCH_ID,
                        CommandToken.FLOW_ID,
                        CommandToken.EXECUTION_ID,
                        CommandToken.BATCH_ARGUMENTS,
                        CommandToken.of(application.getBinaryName()),
                }),
                Arrays.asList());
        HadoopCommandRequired.put(task, false);
    }

    private void addCleanup(M3bpCompilerContext context) {
        context.add(new CleanupStageClientGenerator().generate(
                context.getRoot().getBatchId(),
                context.getInfo().getFlowId(),
                context.getRoot().getOptions().getRuntimeWorkingDirectory(),
                CleanupStageClientGenerator.DEFAULT_CLASS));
    }
}
