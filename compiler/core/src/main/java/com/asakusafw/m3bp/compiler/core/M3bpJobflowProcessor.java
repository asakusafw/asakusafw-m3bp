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
package com.asakusafw.m3bp.compiler.core;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.compiler.codegen.ApplicationGenerator;
import com.asakusafw.dag.compiler.codegen.CleanupStageClientGenerator;
import com.asakusafw.dag.compiler.flow.DataFlowGenerator;
import com.asakusafw.dag.compiler.flow.adapter.ClassGeneratorContextAdapter;
import com.asakusafw.dag.compiler.model.ClassData;
import com.asakusafw.dag.compiler.planner.DagPlanning;
import com.asakusafw.lang.compiler.api.Exclusive;
import com.asakusafw.lang.compiler.api.JobflowProcessor;
import com.asakusafw.lang.compiler.api.reference.CommandToken;
import com.asakusafw.lang.compiler.api.reference.TaskReference;
import com.asakusafw.lang.compiler.common.Diagnostic;
import com.asakusafw.lang.compiler.common.DiagnosticException;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.hadoop.HadoopCommandRequired;
import com.asakusafw.lang.compiler.inspection.InspectionExtension;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.graph.Jobflow;
import com.asakusafw.lang.compiler.model.info.JobflowInfo;
import com.asakusafw.lang.compiler.planning.Plan;
import com.asakusafw.lang.compiler.planning.PlanDetail;
import com.asakusafw.lang.utils.common.Action;
import com.asakusafw.lang.utils.common.Invariants;
import com.asakusafw.m3bp.compiler.common.M3bpPackage;
import com.asakusafw.m3bp.compiler.common.M3bpTask;
import com.asakusafw.m3bp.compiler.comparator.NativeValueComparatorExtension;

/**
 * An implementation of {@link JobflowProcessor} for M3BP.
 */
@Exclusive
public class M3bpJobflowProcessor implements JobflowProcessor {

    static final Logger LOG = LoggerFactory.getLogger(M3bpJobflowProcessor.class);

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
            LOG.debug("generating vertices: {}", source.getFlowId());
            GraphInfo graph = generateGraph(context, source, plan);
            LOG.debug("generating application entry: {}", source.getFlowId());
            addApplication(context, source, graph);
            LOG.debug("generating cleanup : {}", source.getFlowId());
            addCleanup(context, source);
        } finally {
            LOG.debug("generating inspection info: {} ({})",
                    source.getFlowId(), M3bpPackage.PATH_PLAN_INSPECTION);
            InspectionExtension.inspect(context, M3bpPackage.PATH_PLAN_INSPECTION, plan);
        }
    }

    private static Plan plan(Context context, Jobflow source) {
        PlanDetail detail = DagPlanning.plan(context, source);
        return detail.getPlan();
    }

    private static GraphInfo generateGraph(JobflowProcessor.Context context, JobflowInfo info, Plan plan) {
        ClassGeneratorContextAdapter cgContext = new ClassGeneratorContextAdapter(context, M3bpPackage.CLASS_PREFIX);
        NativeValueComparatorExtension comparators = Invariants.requireNonNull(
                context.getExtension(NativeValueComparatorExtension.class));
        M3bpDescriptorFactory descriptors = new M3bpDescriptorFactory(cgContext, comparators);
        return DataFlowGenerator.generate(context, cgContext, descriptors, info, plan);
    }

    private static void addApplication(JobflowProcessor.Context context, JobflowInfo info, GraphInfo graph) {
        LOG.debug("storing GraphInfo ({}): {}", info.getFlowId(), M3bpPackage.PATH_GRAPH_INFO);
        add(context, M3bpPackage.PATH_GRAPH_INFO, output -> GraphInfo.save(output, graph));
        ClassDescription application = add(context, new ApplicationGenerator().generate(
                M3bpPackage.PATH_GRAPH_INFO,
                new ClassDescription(M3bpPackage.CLASS_APPLICATION)));
        LOG.debug("Generating application entry ({}): {}", info.getFlowId(), application.getClassName());
        TaskReference task = context.addTask(
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

    private static void addCleanup(JobflowProcessor.Context context, JobflowInfo info) {
        add(context, new CleanupStageClientGenerator().generate(
                context.getBatchId(),
                info.getFlowId(),
                context.getOptions().getRuntimeWorkingDirectory(),
                CleanupStageClientGenerator.DEFAULT_CLASS));
    }

    private static void add(
            JobflowProcessor.Context context, Location location, Action<OutputStream, IOException> action) {
        try (OutputStream output = context.addResourceFile(location)) {
            action.perform(output);
        } catch (IOException e) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "error occurred while adding a resource: {0}",
                    location), e);
        }
    }

    private static ClassDescription add(JobflowProcessor.Context context, ClassData data) {
        if (data.hasContents()) {
            try (OutputStream output = context.addClassFile(data.getDescription())) {
                data.dump(output);
            } catch (IOException e) {
                throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                        "error occurred while generating a class file: {0}",
                        data.getDescription().getBinaryName()), e);
            }
        }
        return data.getDescription();
    }
}
