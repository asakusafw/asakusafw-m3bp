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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.common.SupplierInfo;
import com.asakusafw.dag.api.model.EdgeDescriptor;
import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.compiler.codegen.BasicSerDeProvider;
import com.asakusafw.dag.compiler.codegen.BufferOperatorGenerator;
import com.asakusafw.dag.compiler.codegen.ClassGeneratorContext;
import com.asakusafw.dag.compiler.codegen.CoGroupInputAdapterGenerator;
import com.asakusafw.dag.compiler.codegen.CompositeOperatorNodeGenerator;
import com.asakusafw.dag.compiler.codegen.EdgeDataTableAdapterGenerator;
import com.asakusafw.dag.compiler.codegen.EdgeOutputAdapterGenerator;
import com.asakusafw.dag.compiler.codegen.ExtractAdapterGenerator;
import com.asakusafw.dag.compiler.codegen.ExtractInputAdapterGenerator;
import com.asakusafw.dag.compiler.codegen.NativeValueComparatorExtension;
import com.asakusafw.dag.compiler.codegen.OperationAdapterGenerator;
import com.asakusafw.dag.compiler.codegen.OperatorNodeGenerator.AggregateNodeInfo;
import com.asakusafw.dag.compiler.codegen.OperatorNodeGenerator.NodeInfo;
import com.asakusafw.dag.compiler.codegen.OperatorNodeGenerator.OperatorNodeInfo;
import com.asakusafw.dag.compiler.codegen.SerDeProvider;
import com.asakusafw.dag.compiler.codegen.VertexAdapterGenerator;
import com.asakusafw.dag.compiler.directio.DirectFileInputAdapterGenerator;
import com.asakusafw.dag.compiler.directio.DirectFileOutputCommitGenerator;
import com.asakusafw.dag.compiler.directio.DirectFileOutputPrepareGenerator;
import com.asakusafw.dag.compiler.directio.DirectFileOutputSetupGenerator;
import com.asakusafw.dag.compiler.directio.OutputPatternSerDeGenerator;
import com.asakusafw.dag.compiler.internalio.InternalInputAdapterGenerator;
import com.asakusafw.dag.compiler.internalio.InternalOutputPrepareGenerator;
import com.asakusafw.dag.compiler.model.build.GraphInfoBuilder;
import com.asakusafw.dag.compiler.model.build.ResolvedInputInfo;
import com.asakusafw.dag.compiler.model.build.ResolvedOutputInfo;
import com.asakusafw.dag.compiler.model.build.ResolvedVertexInfo;
import com.asakusafw.dag.compiler.model.graph.AggregateNode;
import com.asakusafw.dag.compiler.model.graph.DataTableNode;
import com.asakusafw.dag.compiler.model.graph.InputNode;
import com.asakusafw.dag.compiler.model.graph.OperationSpec;
import com.asakusafw.dag.compiler.model.graph.OperatorNode;
import com.asakusafw.dag.compiler.model.graph.OutputNode;
import com.asakusafw.dag.compiler.model.graph.ValueElement;
import com.asakusafw.dag.compiler.model.graph.VertexElement;
import com.asakusafw.dag.compiler.model.graph.VertexElement.ElementKind;
import com.asakusafw.dag.compiler.model.plan.Implementation;
import com.asakusafw.dag.compiler.model.plan.InputSpec;
import com.asakusafw.dag.compiler.model.plan.InputSpec.InputType;
import com.asakusafw.dag.compiler.model.plan.OutputSpec;
import com.asakusafw.dag.compiler.model.plan.OutputSpec.OutputType;
import com.asakusafw.dag.compiler.model.plan.VertexSpec;
import com.asakusafw.dag.compiler.model.plan.VertexSpec.OperationOption;
import com.asakusafw.dag.compiler.model.plan.VertexSpec.OperationType;
import com.asakusafw.dag.runtime.adapter.DataTable;
import com.asakusafw.dag.runtime.directio.DirectFileOutputPrepare;
import com.asakusafw.dag.runtime.internalio.InternalOutputPrepare;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.lang.compiler.api.CompilerOptions;
import com.asakusafw.lang.compiler.api.JobflowProcessor;
import com.asakusafw.lang.compiler.api.reference.DataModelReference;
import com.asakusafw.lang.compiler.api.reference.ExternalInputReference;
import com.asakusafw.lang.compiler.extension.directio.DirectFileInputModel;
import com.asakusafw.lang.compiler.extension.directio.DirectFileIoPortProcessor;
import com.asakusafw.lang.compiler.extension.directio.DirectFileOutputModel;
import com.asakusafw.lang.compiler.extension.directio.OutputPattern;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.description.Descriptions;
import com.asakusafw.lang.compiler.model.description.TypeDescription;
import com.asakusafw.lang.compiler.model.graph.ExternalInput;
import com.asakusafw.lang.compiler.model.graph.ExternalOutput;
import com.asakusafw.lang.compiler.model.graph.Group;
import com.asakusafw.lang.compiler.model.graph.MarkerOperator;
import com.asakusafw.lang.compiler.model.graph.Operator;
import com.asakusafw.lang.compiler.model.graph.OperatorArgument;
import com.asakusafw.lang.compiler.model.graph.OperatorInput;
import com.asakusafw.lang.compiler.model.graph.OperatorOutput;
import com.asakusafw.lang.compiler.model.graph.OperatorPort;
import com.asakusafw.lang.compiler.model.graph.OperatorProperty;
import com.asakusafw.lang.compiler.model.graph.Operators;
import com.asakusafw.lang.compiler.planning.Plan;
import com.asakusafw.lang.compiler.planning.Planning;
import com.asakusafw.lang.compiler.planning.SubPlan;
import com.asakusafw.m3bp.compiler.core.adapter.ClassGeneratorContextAdapter;
import com.asakusafw.m3bp.compiler.core.adapter.OperatorNodeGeneratorContextAdapter;
import com.asakusafw.m3bp.descriptor.Descriptors;
import com.asakusafw.runtime.core.Result;
import com.asakusafw.runtime.flow.VoidResult;
import com.asakusafw.utils.graph.Graph;
import com.asakusafw.utils.graph.Graphs;

/**
 * Generates DAG classes generator.
 */
public final class M3bpDagGenerator {

    static final Logger LOG = LoggerFactory.getLogger(M3bpDagGenerator.class);

    private static final String ID_DIRECT_FILE_OUTPUT_SETUP = "_directio-setup";

    private static final String ID_DIRECT_FILE_OUTPUT_COMMIT = "_directio-commit";

    private static final TypeDescription TYPE_RESULT = Descriptions.typeOf(Result.class);

    private static final TypeDescription TYPE_DATATABLE = Descriptions.typeOf(DataTable.class);

    private static final ClassDescription TYPE_VOID_RESULT = Descriptions.classOf(VoidResult.class);

    private final M3bpCompilerContext context;

    private final Plan plan;

    private final GraphInfoBuilder builder = new GraphInfoBuilder();

    private final ClassGeneratorContext generatorContext;

    private final SerDeProvider serdes;

    private final CompositeOperatorNodeGenerator genericOperators;

    private final ExternalIoAnalyzer externalIo;

    private final NativeValueComparatorExtension comparators;

    private M3bpDagGenerator(M3bpCompilerContext context, Plan plan) {
        this.context = context;
        this.plan = plan;
        this.generatorContext = new ClassGeneratorContextAdapter(context);

        JobflowProcessor.Context root = context.getRoot();
        this.serdes = new BasicSerDeProvider(root.getDataModelLoader(), context::add, context.getClassNameProvider());
        this.genericOperators = CompositeOperatorNodeGenerator.load(context.getRoot().getClassLoader());
        this.externalIo = new ExternalIoAnalyzer(root.getClassLoader(), plan);
        this.comparators = Invariants.requireNonNull(
                context.getRoot().getExtension(NativeValueComparatorExtension.class));
    }

    /**
     * Generates {@link GraphInfo} and its related classes.
     * @param context the current compiler context
     * @param plan the target plan
     * @return the generated {@link GraphInfo}
     */
    public static GraphInfo generate(M3bpCompilerContext context, Plan plan) {
        return new M3bpDagGenerator(context, plan).generate();
    }

    private GraphInfo generate() {
        GraphInfo graph = generateGraph();
        return graph;
    }

    private GraphInfo generateGraph() {
        resolveOutputs();
        resolveOperations();
        return builder.build(Descriptors::newVoidEdge);
    }

    private void resolveOperations() {
        Graph<SubPlan> graph = Planning.toDependencyGraph(plan);
        Graph<SubPlan> rev = Graphs.transpose(graph);
        for (SubPlan sub : Graphs.sortPostOrder(rev)) {
            VertexSpec spec = VertexSpec.get(sub);
            if (externalIo.getOutputMap().containsKey(sub)) {
                continue;
            }
            resolveOperation(spec);
        }
    }

    private void resolveOperation(VertexSpec vertex) {
        LOG.debug("compiling operation vertex: {} ({})", vertex.getId(), vertex.getLabel()); //$NON-NLS-1$
        Map<Operator, VertexElement> resolved = resolveVertexElements(vertex);
        ClassDescription inputAdapter = resolveInputAdapter(resolved, vertex);
        List<ClassDescription> dataTableAdapters = resolveDataTableAdapters(resolved, vertex);
        ClassDescription operationAdapter = resolveOperationAdapter(resolved, vertex);
        ClassDescription outputAdapter = resolveOutputAdapter(resolved, vertex);
        ClassDescription vertexClass = context.generate(q(vertex, "vertex"), c -> {
            return new VertexAdapterGenerator().generate(
                    generatorContext,
                    inputAdapter,
                    dataTableAdapters,
                    operationAdapter,
                    Collections.singletonList(outputAdapter),
                    vertex.getLabel(),
                    c);
        });
        Map<SubPlan.Input, ResolvedInputInfo> inputs = collectInputs(resolved, vertex);
        Map<SubPlan.Output, ResolvedOutputInfo> outputs = collectOutputs(vertex);
        ResolvedVertexInfo info = new ResolvedVertexInfo(
                vertex.getId(),
                Descriptors.newVertex(supplier(vertexClass)),
                inputs,
                outputs,
                Collections.emptySet());
        bless(vertex, info, vertexClass);
    }

    private Map<Operator, VertexElement> resolveVertexElements(VertexSpec vertex) {
        Map<Operator, VertexElement> resolved = new HashMap<>();
        for (SubPlan.Input port : vertex.getOrigin().getInputs()) {
            InputSpec spec = InputSpec.get(port);
            if (spec.getInputType() != InputType.BROADCAST) {
                continue;
            }
            resolved.put(port.getOperator(), new DataTableNode(spec.getId(), TYPE_DATATABLE, spec.getDataType()));
        }
        Graph<Operator> graph = Graphs.transpose(Planning.toDependencyGraph(vertex.getOrigin()));
        for (Operator operator : Graphs.sortPostOrder(graph)) {
            resolveBodyOperator(resolved, vertex, operator);
        }
        return resolved;
    }

    private ClassDescription resolveInputAdapter(Map<Operator, VertexElement> resolved, VertexSpec vertex) {
        Operator primary = vertex.getPrimaryOperator();
        if (vertex.getOperationOptions().contains(OperationOption.EXTERNAL_INPUT)) {
            Invariants.require(primary instanceof ExternalInput);
            ExternalInput input = (ExternalInput) primary;
            VertexElement driver = resolveExtractDriver(resolved, vertex, input.getOperatorPort());
            resolved.put(input, new InputNode(driver));
            return resolveExternalInput(vertex, input);
        } else if (vertex.getOperationType() == OperationType.CO_GROUP) {
            Invariants.requireNonNull(primary);
            VertexElement element = resolved.get(primary);
            Invariants.requireNonNull(element);

            List<InputSpec> inputs = new ArrayList<>();
            for (OperatorInput input : primary.getInputs()) {
                Collection<OperatorOutput> opposites = input.getOpposites();
                if (opposites.isEmpty()) {
                    continue;
                }
                Invariants.require(opposites.size() == 1);
                opposites.stream()
                        .map(OperatorPort::getOwner)
                        .map(p -> Invariants.requireNonNull(vertex.getOrigin().findInput(p)))
                        .map(InputSpec::get)
                        .filter(s -> s.getInputType() == InputType.CO_GROUP)
                        .forEach(inputs::add);
            }
            Invariants.require(inputs.size() >= 1);

            // Note: only add the first input
            resolved.put(inputs.get(0).getOrigin().getOperator(), new InputNode(element));
            return resolveCoGroupInput(vertex, inputs);
        } else {
            List<InputSpec> inputs = vertex.getOrigin().getInputs().stream()
                .map(InputSpec::get)
                .filter(s -> s.getInputType() == InputType.EXTRACT)
                .collect(Collectors.toList());
            Invariants.require(inputs.size() == 1);
            MarkerOperator edge = inputs.get(0).getOrigin().getOperator();
            VertexElement driver = resolveExtractDriver(resolved, vertex, edge.getOutput());
            resolved.put(edge, new InputNode(driver));
            return resolveExtractInput(inputs.get(0));
        }
    }

    private VertexElement resolveExtractDriver(
            Map<Operator, VertexElement> resolved, VertexSpec vertex, OperatorOutput output) {
        VertexElement succ = resolveSuccessors(resolved, vertex, output);
        ClassDescription gen = context.generate(q(vertex, "operator"), c -> {
            return new ExtractAdapterGenerator().generate(succ, c);
        });
        return new OperatorNode(gen, TYPE_RESULT, output.getDataType(), succ);
    }

    private ClassDescription resolveExtractInput(InputSpec spec) {
        ExtractInputAdapterGenerator.Spec s = new ExtractInputAdapterGenerator.Spec(spec.getId(), spec.getDataType());
        return context.generate(q(spec.getOrigin().getOwner(), "adapter.input"), c -> {
            return new ExtractInputAdapterGenerator().generate(generatorContext, s, c);
        });
    }

    private ClassDescription resolveCoGroupInput(VertexSpec vertex, List<InputSpec> inputs) {
        List<CoGroupInputAdapterGenerator.Spec> specs = inputs.stream()
                .map(s -> new CoGroupInputAdapterGenerator.Spec(
                        s.getId(),
                        s.getDataType(),
                        s.getInputOptions().contains(InputSpec.InputOption.SPILL_OUT)))
                .collect(Collectors.toList());
        return context.generate(q(vertex, "adapter.input"), c -> {
            return new CoGroupInputAdapterGenerator().generate(generatorContext, specs, c);
        });
    }

    private ClassDescription resolveExternalInput(VertexSpec vertex, ExternalInput input) {
        if (externalIo.getInternalInputs().containsKey(input)) {
            String path = externalIo.getInternalInputs().get(input);
            return resolveInternalInput(vertex, input, Collections.singleton(path));
        } else if (externalIo.getDirectInputs().containsKey(input)) {
            DirectFileInputModel model = externalIo.getDirectInputs().get(input);
            return resolveDirectInput(vertex, input, model);
        } else {
            Invariants.require(externalIo.getGenericInputs().contains(input));
            ExternalInputReference ref = context.getRoot().addExternalInput(vertex.getId(), input.getInfo());
            return resolveInternalInput(vertex, input, ref.getPaths());
        }
    }

    private ClassDescription resolveInternalInput(VertexSpec vertex, ExternalInput input, Collection<String> paths) {
        InternalInputAdapterGenerator.Spec spec = new InternalInputAdapterGenerator.Spec(
                input.getName(), paths, input.getDataType());
        return context.generate(q(vertex, "input.internalio"), c -> {
            return new InternalInputAdapterGenerator().generate(generatorContext, spec, c);
        });
    }

    private ClassDescription resolveDirectInput(VertexSpec vertex, ExternalInput input, DirectFileInputModel model) {
        ClassDescription filterClass = model.getFilterClass();
        if (filterClass != null && isDirectInputFilterEnabled() == false) {
            LOG.info(MessageFormat.format(
                    "Direct I/O input filter is disabled in current setting: {0} ({1})",
                    input.getInfo().getDescriptionClass().getClassName(),
                    filterClass.getClassName()));
            filterClass = null;
        }
        DirectFileInputAdapterGenerator.Spec spec = new DirectFileInputAdapterGenerator.Spec(
                input.getName(),
                model.getBasePath(),
                model.getResourcePattern(),
                model.getFormatClass(),
                filterClass,
                model.isOptional());
        return context.generate(q(vertex, "input.directio"), c -> {
            return new DirectFileInputAdapterGenerator().generate(generatorContext, spec, c);
        });
    }

    private ClassDescription resolveOutputAdapter(Map<Operator, VertexElement> resolved, VertexSpec vertex) {
        List<EdgeOutputAdapterGenerator.Spec> specs = new ArrayList<>();
        for (SubPlan.Output port : vertex.getOrigin().getOutputs()) {
            if (resolved.containsKey(port.getOperator()) == false) {
                continue;
            }
            OutputSpec spec = OutputSpec.get(port);
            if (spec.getOutputType() == OutputType.DISCARD) {
                continue;
            }
            ClassDescription mapperClass = null;
            ClassDescription copierClass = null;
            ClassDescription combinerClass = null;
            Set<? extends SubPlan.Input> opposites = port.getOpposites();
            if (opposites.isEmpty() == false) {
                SubPlan.Input first = opposites.stream().findFirst().get();
                ResolvedInputInfo downstream = builder.get(first);
                mapperClass = downstream.getMapperType();
                copierClass = downstream.getCopierType();
                combinerClass = downstream.getCombinerType();
            }
            specs.add(new EdgeOutputAdapterGenerator.Spec(
                    spec.getId(), spec.getDataType(),
                    mapperClass, copierClass, combinerClass));
        }
        return context.generate(q(vertex, "adapter.output"), c -> {
            return new EdgeOutputAdapterGenerator().generate(generatorContext, specs, c);
        });
    }

    private List<ClassDescription> resolveDataTableAdapters(Map<Operator, VertexElement> resolved, VertexSpec vertex) {
        List<EdgeDataTableAdapterGenerator.Spec> specs = new ArrayList<>();
        for (SubPlan.Input port : vertex.getOrigin().getInputs()) {
            if (resolved.containsKey(port.getOperator()) == false) {
                continue;
            }
            InputSpec spec = InputSpec.get(port);
            if (spec.getInputType() != InputType.BROADCAST) {
                continue;
            }
            Group group = Invariants.requireNonNull(spec.getPartitionInfo());
            specs.add(new EdgeDataTableAdapterGenerator.Spec(spec.getId(), spec.getId(), spec.getDataType(), group));
        }
        if (specs.isEmpty()) {
            return Collections.emptyList();
        }
        ClassDescription generated = context.generate(q(vertex, "adapter.table"), c -> {
            return new EdgeDataTableAdapterGenerator().generate(generatorContext, specs, c);
        });
        return Collections.singletonList(generated);
    }

    private ClassDescription resolveOperationAdapter(Map<Operator, VertexElement> resolved, VertexSpec vertex) {
        return context.generate(q(vertex, "adapter.operation"), c -> {
            OperationSpec operation = null;
            for (VertexElement element : resolved.values()) {
                if (element instanceof InputNode) {
                    Invariants.require(operation == null);
                    operation = new OperationSpec((InputNode) element);
                }
            }
            Invariants.requireNonNull(operation);
            return new OperationAdapterGenerator().generate(generatorContext, operation, c);
        });
    }

    private void resolveBodyOperator(Map<Operator, VertexElement> resolved, VertexSpec vertex, Operator operator) {
        switch (operator.getOperatorKind()) {
        case CORE:
        case USER:
            resolveGeneralOperator(resolved, vertex, operator);
            break;
        case MARKER:
            if (vertex.getOrigin().findOutput(operator) != null) {
                resolveEdgeOutput(resolved, vertex, vertex.getOrigin().findOutput(operator));
            }
            break;
        default:
            break;
        }
    }

    private void resolveGeneralOperator(Map<Operator, VertexElement> resolved, VertexSpec vertex, Operator operator) {
        Map<OperatorProperty, VertexElement> dependencies = new LinkedHashMap<>();
        // add broadcast inputs as data tables
        for (OperatorInput port : operator.getInputs()) {
            if (port.getOpposites().size() != 1) {
                continue;
            }
            VertexElement upstream = port.getOpposites().stream()
                .map(OperatorPort::getOwner)
                .map(resolved::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
            if (upstream != null && upstream.getElementKind() == ElementKind.DATA_TABLE) {
                dependencies.put(port, upstream);
            }
        }
        // add outputs as succeeding result sinks
        for (OperatorOutput port : operator.getOutputs()) {
            VertexElement successor = resolveSuccessors(resolved, vertex, port);
            dependencies.put(port, successor);
        }
        // add arguments as constant values
        for (OperatorArgument arg : operator.getArguments()) {
            VertexElement value = new ValueElement(arg.getValue());
            dependencies.put(arg, value);
        }
        OperatorNodeGeneratorContextAdapter adapter =
                new OperatorNodeGeneratorContextAdapter(generatorContext, vertex.getOrigin(), dependencies);
        context.generate(q(vertex, "operator"), c -> {
            NodeInfo info = genericOperators.generate(adapter, operator, c);
            VertexElement element = resolveNodeInfo(vertex, operator, info);
            resolved.put(operator, element);
            return info.getClassData();
        });
    }

    private VertexElement resolveNodeInfo(VertexSpec vertex, Operator operator, NodeInfo info) {
        if (info instanceof OperatorNodeInfo) {
            return new OperatorNode(
                    info.getClassData().getDescription(),
                    TYPE_RESULT,
                    info.getDataType(),
                    info.getDependencies());
        } else if (info instanceof AggregateNodeInfo) {
            AggregateNodeInfo aggregate = (AggregateNodeInfo) info;
            boolean combine = vertex.getOperationOptions().contains(OperationOption.PRE_AGGREGATION);
            return new AggregateNode(
                    info.getClassData().getDescription(),
                    TYPE_RESULT,
                    aggregate.getMapperType(),
                    combine ? aggregate.getCopierType() : null,
                    combine ? aggregate.getCombinerType() : null,
                    aggregate.getInputType(),
                    aggregate.getOutputType(),
                    info.getDependencies());
        } else {
            throw new AssertionError(info);
        }
    }

    private VertexElement resolveSuccessors(
            Map<Operator, VertexElement> resolved, VertexSpec vertex, OperatorOutput port) {
        List<VertexElement> succs = Operators.getSuccessors(Collections.singleton(port)).stream()
                .map(o -> Invariants.requireNonNull(resolved.get(o)))
                .collect(Collectors.toList());
        if (succs.size() == 0) {
            return new OperatorNode(TYPE_VOID_RESULT, TYPE_RESULT, port.getDataType(), Collections.emptyList());
        } else if (succs.size() == 1) {
            return succs.get(0);
        } else {
            ClassDescription generated = context.generate(q(vertex, "operator"), c -> {
                return new BufferOperatorGenerator().generate(succs, c);
            });
            return new OperatorNode(generated, TYPE_RESULT, port.getDataType(), succs);
        }
    }

    private void resolveEdgeOutput(Map<Operator, VertexElement> resolved, VertexSpec vertex, SubPlan.Output port) {
        OutputSpec spec = OutputSpec.get(port);
        VertexElement element;
        if (spec.getOutputType() == OutputType.DISCARD) {
            element = new OperatorNode(TYPE_VOID_RESULT, TYPE_RESULT, spec.getSourceType(), Collections.emptyList());
        } else {
            element = new OutputNode(spec.getId(), TYPE_RESULT, spec.getSourceType());
        }
        MarkerOperator operator = port.getOperator();
        resolved.put(operator, element);
    }

    private Map<SubPlan.Input, ResolvedInputInfo> collectInputs(
            Map<Operator, VertexElement> resolved, VertexSpec vertex) {
        Map<SubPlan.Input, ResolvedInputInfo> results = new LinkedHashMap<>();
        for (SubPlan.Input port : vertex.getOrigin().getInputs()) {
            InputSpec spec = InputSpec.get(port);
            InputType type = spec.getInputType();
            if (type == InputType.EXTRACT) {
                ClassDescription serde = getValueSerDe(spec.getDataType());
                ResolvedInputInfo info = new ResolvedInputInfo(
                        spec.getId(),
                        Descriptors.newOneToOneEdge(supplier(serde)));
                results.put(port, info);
            } else if (type == InputType.BROADCAST) {
                ClassDescription serde = getValueSerDe(spec.getDataType());
                ResolvedInputInfo info = new ResolvedInputInfo(
                        spec.getId(),
                        Descriptors.newBroadcastEdge(supplier(serde)));
                results.put(port, info);
            } else if (type == InputType.CO_GROUP) {
                ClassDescription serde = getKeyValueSerDe(spec.getDataType(), spec.getPartitionInfo());
                String comparator = getComparator(spec.getDataType(), spec.getPartitionInfo().getOrdering());
                ClassDescription mapperType = null;
                ClassDescription copierType = null;
                ClassDescription combinerType = null;
                Operator operator = Invariants.requireNonNull(vertex.getPrimaryOperator());
                VertexElement element = Invariants.requireNonNull(resolved.get(operator));
                if (element instanceof AggregateNode) {
                    AggregateNode aggregate = (AggregateNode) element;
                    mapperType = aggregate.getMapperType();
                    copierType = aggregate.getCopierType();
                    combinerType = aggregate.getCombinerType();
                }
                ResolvedInputInfo info = new ResolvedInputInfo(
                        spec.getId(), Descriptors.newScatterGatherEdge(supplier(serde), comparator),
                        mapperType, copierType, combinerType);
                results.put(port, info);
            }
        }
        return results;
    }

    private Map<SubPlan.Output, ResolvedOutputInfo> collectOutputs(VertexSpec vertex) {
        Map<SubPlan.Output, ResolvedOutputInfo> results = new LinkedHashMap<>();
        for (SubPlan.Output port : vertex.getOrigin().getOutputs()) {
            OutputSpec spec = OutputSpec.get(port);
            if (spec.getOutputType() == OutputType.DISCARD) {
                continue;
            }
            Set<ResolvedInputInfo> downstreams = port.getOpposites().stream()
                    .map(p -> Invariants.requireNonNull(builder.get(p)))
                    .collect(Collectors.toSet());
            ResolvedOutputInfo info = new ResolvedOutputInfo(spec.getId(), downstreams);
            results.put(port, info);
        }
        return results;
    }

    private void resolveOutputs() {
        for (Map.Entry<SubPlan, ExternalOutput> entry : externalIo.getOutputMap().entrySet()) {
            VertexSpec vertex = VertexSpec.get(entry.getKey());
            ExternalOutput output = entry.getValue();
            if (externalIo.getInternalOutputs().containsKey(output)) {
                if (isEmptyOutput(vertex)) {
                    continue;
                }
                String path = externalIo.getInternalOutputs().get(output);
                resolveInternalOutput(vertex, output, path);
            } else if (externalIo.getGenericOutputs().contains(output)) {
                resolveGenericOutput(vertex, output);
            } else {
                Invariants.require(externalIo.getDirectOutputs().containsKey(output));
            }
        }
        resolveDirectFileOutputs();
    }

    private void resolveGenericOutput(VertexSpec vertex, ExternalOutput port) {
        LOG.debug("resolving generic output vertex: {} ({})", vertex.getId(), vertex.getLabel()); //$NON-NLS-1$
        if (isEmptyOutput(vertex)) {
            context.getRoot().addExternalOutput(
                    port.getName(),
                    port.getInfo(),
                    Collections.emptyList());
        } else {
            CompilerOptions options = context.getRoot().getOptions();
            String path = options.getRuntimeWorkingPath(String.format("%s/part-*", //$NON-NLS-1$
                    port.getName()));
            context.getRoot().addExternalOutput(
                    port.getName(),
                    port.getInfo(),
                    Arrays.asList(path));
            registerInternalOutput(vertex, port, path);
        }
    }

    private void resolveInternalOutput(VertexSpec vertex, ExternalOutput port, String path) {
        LOG.debug("resolving internal output vertex: {} ({})", vertex.getId(), vertex.getLabel()); //$NON-NLS-1$
        registerInternalOutput(vertex, port, path);
    }

    private void resolveDirectFileOutputs() {
        ResolvedVertexInfo setup = registerDirectOutputSetup();
        if (setup == null) {
            return;
        }
        List<ResolvedVertexInfo> prepares = new ArrayList<>();
        for (Map.Entry<SubPlan, ExternalOutput> entry : externalIo.getOutputMap().entrySet()) {
            ExternalOutput output = entry.getValue();
            if (externalIo.getDirectOutputs().containsKey(output) == false) {
                continue;
            }
            VertexSpec vertex = VertexSpec.get(entry.getKey());
            if (isEmptyOutput(vertex)) {
                continue;
            }
            DirectFileOutputModel model = Invariants.requireNonNull(externalIo.getDirectOutputs().get(output));
            ResolvedVertexInfo v = registerDirectOutputPrepare(vertex, output, model, setup);
            prepares.add(v);
        }
        registerDirectOutputCommit(prepares);
    }

    private ResolvedVertexInfo registerInternalOutput(VertexSpec vertex, ExternalOutput output, String path) {
        List<InternalOutputPrepareGenerator.Spec> specs = new ArrayList<>();
        specs.add(new InternalOutputPrepareGenerator.Spec(output.getName(), path, output.getDataType()));
        ClassDescription serde = getValueSerDe(output.getDataType());
        ClassDescription vertexClass = context.generate(q(vertex, "output.internal"), c -> { //$NON-NLS-1$
            InternalOutputPrepareGenerator generator = new InternalOutputPrepareGenerator();
            return generator.generate(generatorContext, specs, c);
        });
        SubPlan.Input entry = externalIo.getOutputSource(vertex.getOrigin());
        ResolvedInputInfo input = new ResolvedInputInfo(
                InternalOutputPrepare.INPUT_NAME,
                Descriptors.newOneToOneEdge(supplier(serde)));
        ResolvedVertexInfo info = new ResolvedVertexInfo(
                vertex.getId(),
                Descriptors.newVertex(supplier(vertexClass)),
                Collections.singletonMap(entry, input),
                Collections.emptyMap(),
                Collections.emptySet());
        return bless(vertex, info, vertexClass);
    }

    private ResolvedVertexInfo registerDirectOutputSetup() {
        List<DirectFileOutputSetupGenerator.Spec> specs = externalIo.getDirectOutputs().entrySet().stream()
                .map(e -> new DirectFileOutputSetupGenerator.Spec(
                        e.getKey().getName(),
                        e.getValue().getBasePath(),
                        e.getValue().getDeletePatterns()))
                .collect(Collectors.toList());
        if (specs.isEmpty()) {
            return null;
        }
        LOG.debug("resolving Direct I/O file output setup vertex: {} ({})", //$NON-NLS-1$
                ID_DIRECT_FILE_OUTPUT_SETUP, specs.size());
        ClassDescription proc = context.generate("directio.setup", c -> {
            DirectFileOutputSetupGenerator generator = new DirectFileOutputSetupGenerator();
            return generator.generate(generatorContext, specs, c);
        });
        ResolvedVertexInfo info = new ResolvedVertexInfo(
                ID_DIRECT_FILE_OUTPUT_SETUP,
                Descriptors.newVertex(supplier(proc)),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet());
        return bless(info);
    }

    private ResolvedVertexInfo registerDirectOutputPrepare(
            VertexSpec vertex, ExternalOutput output,
            DirectFileOutputModel model, ResolvedVertexInfo setup) {
        LOG.debug("resolving Direct I/O file output prepare vertex: {} ({})", //$NON-NLS-1$
                vertex.getId(), vertex.getLabel());
        DataModelReference ref = context.getRoot().getDataModelLoader().load(output.getDataType());
        OutputPattern pattern = OutputPattern.compile(ref, model.getResourcePattern(), model.getOrder());
        boolean gather = pattern.isGatherRequired();
        List<DirectFileOutputPrepareGenerator.Spec> specs = new ArrayList<>();
        specs.add(new DirectFileOutputPrepareGenerator.Spec(
                output.getName(),
                model.getBasePath(),
                gather ? null : model.getResourcePattern(),
                model.getFormatClass()));
        EdgeDescriptor edge;
        if (gather) {
            ClassDescription serde = context.generate(q(vertex, "serde.directio"), c -> { //$NON-NLS-1$
                OutputPatternSerDeGenerator generator = new OutputPatternSerDeGenerator();
                return generator.generate(ref, pattern, c);
            });
            List<Group.Ordering> orderings = pattern.getOrders().stream()
                    .map(o -> new Group.Ordering(
                            o.getTarget().getName(),
                            o.isAscend() ? Group.Direction.ASCENDANT : Group.Direction.DESCENDANT))
                    .collect(Collectors.toList());
            String comparator = getComparator(ref.getDeclaration(), orderings);
            edge = Descriptors.newScatterGatherEdge(supplier(serde), comparator);
        } else {
            edge = Descriptors.newOneToOneEdge(supplier(getValueSerDe(output.getDataType())));
        }
        ClassDescription vertexClass = context.generate(q(vertex, "output.directio"), c -> { //$NON-NLS-1$
            DirectFileOutputPrepareGenerator generator = new DirectFileOutputPrepareGenerator();
            return generator.generate(generatorContext, specs, c);
        });
        SubPlan.Input entry = externalIo.getOutputSource(vertex.getOrigin());
        ResolvedInputInfo input = new ResolvedInputInfo(
                DirectFileOutputPrepare.INPUT_NAME,
                edge);
        ResolvedVertexInfo info = new ResolvedVertexInfo(
                vertex.getId(),
                Descriptors.newVertex(supplier(vertexClass)),
                Collections.singletonMap(entry, input),
                Collections.emptyMap(),
                Collections.singleton(setup));
        return bless(vertex, info, vertexClass);
    }

    private ResolvedVertexInfo registerDirectOutputCommit(Collection<? extends ResolvedVertexInfo> prepares) {
        List<DirectFileOutputCommitGenerator.Spec> specs = externalIo.getDirectOutputs().entrySet().stream()
                .map(e -> new DirectFileOutputCommitGenerator.Spec(e.getKey().getName(), e.getValue().getBasePath()))
                .collect(Collectors.toList());
        if (specs.isEmpty()) {
            return null;
        }
        LOG.debug("resolving Direct I/O file output commit vertex: {} ({})", //$NON-NLS-1$
                ID_DIRECT_FILE_OUTPUT_COMMIT, specs.size());
        ClassDescription proc = context.generate("directio.commit", c -> {
            DirectFileOutputCommitGenerator generator = new DirectFileOutputCommitGenerator();
            return generator.generate(generatorContext, specs, c);
        });
        ResolvedVertexInfo vertex = new ResolvedVertexInfo(
                ID_DIRECT_FILE_OUTPUT_COMMIT,
                Descriptors.newVertex(supplier(proc)),
                Collections.emptyMap(),
                Collections.emptyMap(),
                prepares);
        return bless(vertex);
    }

    private ResolvedVertexInfo bless(ResolvedVertexInfo vertex) {
        builder.add(vertex);
        return vertex;
    }

    private ResolvedVertexInfo bless(VertexSpec vertex, ResolvedVertexInfo info, ClassDescription processor) {
        SubPlan origin = vertex.getOrigin();
        origin.putAttribute(Implementation.class, new Implementation(processor));
        builder.add(origin, info);
        return info;
    }

    private ClassDescription getValueSerDe(TypeDescription dataType) {
        return serdes.getValueSerDe(dataType);
    }

    private ClassDescription getKeyValueSerDe(TypeDescription dataType, Group group) {
        return serdes.getKeyValueSerDe(dataType, group);
    }

    private String getComparator(TypeDescription type, List<Group.Ordering> orderings) {
        return comparators.addComparator(type, new Group(Collections.emptyList(), orderings));
    }

    private SupplierInfo supplier(ClassDescription aClass) {
        return SupplierInfo.of(aClass.getBinaryName());
    }

    private String q(VertexSpec vertex, String name) {
        return String.format("%s.%s", vertex.getId(), name);
    }

    private String q(SubPlan element, String name) {
        return q(VertexSpec.get(element), name);
    }

    private boolean isEmptyOutput(VertexSpec vertex) {
        if (vertex.getOperationType() != OperationType.OUTPUT) {
            return false;
        }
        return vertex.getOrigin().getInputs().stream()
            .map(InputSpec::get)
            .anyMatch(p -> p.getInputType() != InputType.NO_DATA) == false;
    }

    private boolean isDirectInputFilterEnabled() {
        return context.getRoot().getOptions().get(
                DirectFileIoPortProcessor.OPTION_FILTER_ENABLED,
                DirectFileIoPortProcessor.DEFAULT_FILTER_ENABLED);
    }
}
