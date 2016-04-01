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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.lang.compiler.extension.directio.DirectFileInputModel;
import com.asakusafw.lang.compiler.extension.directio.DirectFileOutputModel;
import com.asakusafw.lang.compiler.internalio.InternalIoConstants;
import com.asakusafw.lang.compiler.model.description.ValueDescription;
import com.asakusafw.lang.compiler.model.graph.ExternalInput;
import com.asakusafw.lang.compiler.model.graph.ExternalOutput;
import com.asakusafw.lang.compiler.model.graph.ExternalPort;
import com.asakusafw.lang.compiler.model.graph.Operator;
import com.asakusafw.lang.compiler.model.graph.Operators;
import com.asakusafw.lang.compiler.model.info.ExternalPortInfo;
import com.asakusafw.lang.compiler.planning.Plan;
import com.asakusafw.lang.compiler.planning.SubPlan;

/**
 * Utilities for special I/O.
 */
public class ExternalIoAnalyzer {

    static final Logger LOG = LoggerFactory.getLogger(ExternalIoAnalyzer.class);

    /**
     * The module name of Direct I/O.
     */
    public static final String DIRECTIO_MODULE_NAME = "directio";

    private final Map<SubPlan, ExternalOutput> outputMap;

    private final Map<ExternalInput, DirectFileInputModel> directInputs;

    private final Map<ExternalOutput, DirectFileOutputModel> directOutputs;

    private final Map<ExternalInput, String> internalInputs;

    private final Map<ExternalOutput, String> internalOutputs;

    private final Set<ExternalInput> genericInputs;

    private final Set<ExternalOutput> genericOutputs;

    /**
     * Creates a new instance.
     * @param loader the target class loader
     * @param plan the target plan
     */
    public ExternalIoAnalyzer(ClassLoader loader, Plan plan) {
        Arguments.requireNonNull(loader);
        Arguments.requireNonNull(plan);
        this.outputMap = buildOutputMap(plan);

        Set<ExternalInput> inputs = collect(loader, plan, ExternalInput.class);
        Set<ExternalOutput> outputs = collect(loader, plan, ExternalOutput.class);
        this.directInputs = collect(loader, inputs, DIRECTIO_MODULE_NAME, DirectFileInputModel.class);
        this.directOutputs = collect(loader, outputs, DIRECTIO_MODULE_NAME, DirectFileOutputModel.class);
        this.internalInputs = collect(loader, inputs, InternalIoConstants.MODULE_NAME, String.class);
        this.internalOutputs = collect(loader, outputs, InternalIoConstants.MODULE_NAME, String.class);
        inputs.removeIf(p -> directInputs.containsKey(p) || internalInputs.containsKey(p));
        outputs.removeIf(p -> directOutputs.containsKey(p) || internalOutputs.containsKey(p));
        this.genericInputs = Collections.unmodifiableSet(inputs);
        this.genericOutputs = Collections.unmodifiableSet(outputs);
    }

    private Map<SubPlan, ExternalOutput> buildOutputMap(Plan plan) {
        Map<SubPlan, ExternalOutput> results = new LinkedHashMap<>();
        for (SubPlan sub : plan.getElements()) {
            ExternalOutput output = findExternalOutput(sub);
            if (output != null) {
                results.put(sub, output);
            }
        }
        return results;
    }

    /**
     * Returns the source port of the target output sub-plan.
     * @param sub the target sub-plan
     * @return the source port
     */
    public SubPlan.Input getOutputSource(SubPlan sub) {
        Invariants.require(outputMap.containsKey(sub));
        List<SubPlan.Input> ports = Operators.getPredecessors(outputMap.get(sub))
                .stream()
                .map(o -> Invariants.requireNonNull(sub.findInput(o)))
                .collect(Collectors.toList());
        Invariants.require(ports.size() == 1);
        return ports.get(0);
    }

    /**
     * Returns the output map.
     * @return the output map
     */
    public Map<SubPlan, ExternalOutput> getOutputMap() {
        return outputMap;
    }

    /**
     * Returns the direct inputs.
     * @return the direct inputs
     */
    public Map<ExternalInput, DirectFileInputModel> getDirectInputs() {
        return directInputs;
    }

    /**
     * Returns the internal inputs.
     * @return the internal inputs
     */
    public Map<ExternalInput, String> getInternalInputs() {
        return internalInputs;
    }

    /**
     * Returns the generic inputs.
     * @return the generic inputs
     */
    public Set<ExternalInput> getGenericInputs() {
        return genericInputs;
    }

    /**
     * Returns the direct outputs.
     * @return the direct outputs
     */
    public Map<ExternalOutput, DirectFileOutputModel> getDirectOutputs() {
        return directOutputs;
    }

    /**
     * Returns the internal outputs.
     * @return the internal outputs
     */
    public Map<ExternalOutput, String> getInternalOutputs() {
        return internalOutputs;
    }

    /**
     * Returns the generic outputs.
     * @return the generic outputs
     */
    public Set<ExternalOutput> getGenericOutputs() {
        return genericOutputs;
    }

    private ExternalOutput findExternalOutput(SubPlan sub) {
        boolean sawOther = false;
        ExternalOutput result = null;
        for (Operator operator : sub.getOperators()) {
            switch (operator.getOperatorKind()) {
            case OUTPUT:
                Invariants.require(result == null);
                result = (ExternalOutput) operator;
                break;
            case INPUT:
            case CORE:
            case USER:
            case CUSTOM:
                sawOther = true;
                break;
            case MARKER:
                break;
            default:
                throw new AssertionError(operator);
            }
        }
        ExternalOutput o = result;
        Invariants.require(result == null || sawOther == false, () -> o);
        return result;
    }

    private <P extends ExternalPort> Set<P> collect(ClassLoader loader, Plan plan, Class<P> portType) {
        return plan.getElements().stream()
                .flatMap(s -> s.getOperators().stream())
                .filter(portType::isInstance)
                .map(portType::cast)
                .filter(ExternalPort::isExternal)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <P extends ExternalPort, M> Map<P, M> collect(
            ClassLoader classLoader, Collection<P> ports,
            String moduleName, Class<M> modelType) {
        Map<P, M> results = new LinkedHashMap<>();
        for (P port : ports) {
            ExternalPortInfo info = port.getInfo();
            if (info == null || info.getModuleName().equals(moduleName) == false) {
                continue;
            }
            ValueDescription contents = info.getContents();
            if (contents != null) {
                try {
                    Object model = contents.resolve(classLoader);
                    if (modelType.isInstance(model)) {
                        results.put(port, modelType.cast(model));
                    }
                } catch (ReflectiveOperationException e) {
                    LOG.warn(MessageFormat.format(
                            "failed to resolve external port: {0} ({1})",
                            port, info.getModuleName()), e);
                }
            }
        }
        return Collections.unmodifiableMap(results);
    }
}
