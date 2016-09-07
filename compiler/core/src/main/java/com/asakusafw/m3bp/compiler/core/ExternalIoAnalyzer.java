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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.compiler.jdbc.windgate.WindGateJdbcInputModel;
import com.asakusafw.dag.compiler.jdbc.windgate.WindGateJdbcIoAnalyzer;
import com.asakusafw.dag.compiler.jdbc.windgate.WindGateJdbcModel;
import com.asakusafw.dag.compiler.jdbc.windgate.WindGateJdbcOutputModel;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.lang.compiler.api.CompilerOptions;
import com.asakusafw.lang.compiler.common.NamePattern;
import com.asakusafw.lang.compiler.extension.directio.DirectFileInputModel;
import com.asakusafw.lang.compiler.extension.directio.DirectFileIoConstants;
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
 * @since 0.1.0
 * @version 0.2.0
 */
class ExternalIoAnalyzer {

    static final Logger LOG = LoggerFactory.getLogger(ExternalIoAnalyzer.class);

    private final Map<SubPlan, ExternalInput> inputMap;

    private final Map<SubPlan, ExternalOutput> outputMap;

    private final ExternalPortMap<DirectFileInputModel, DirectFileOutputModel> directFile;

    private final ExternalPortMap<WindGateJdbcInputModel, WindGateJdbcOutputModel> windgateJdbc;

    private final ExternalPortMap<String, String> internal;

    private final ExternalPortMap<?, ?> generic;

    /**
     * Creates a new instance.
     * @param options the compiler options
     * @param loader the target class loader
     * @param plan the target plan
     */
    ExternalIoAnalyzer(CompilerOptions options, ClassLoader loader, Plan plan) {
        Arguments.requireNonNull(options);
        Arguments.requireNonNull(loader);
        Arguments.requireNonNull(plan);
        this.inputMap = collectUnique(plan, ExternalInput.class);
        this.outputMap = collectUnique(plan, ExternalOutput.class);

        Set<ExternalInput> inputs = collect(plan, ExternalInput.class);
        Set<ExternalOutput> outputs = collect(plan, ExternalOutput.class);
        Predicate<WindGateJdbcModel> windgateJdbcFilter = buildWindGateJdbcFilter(options);
        this.directFile = ExternalPortMap.collect(inputs, outputs,
                extractor(loader, DirectFileIoConstants.MODULE_NAME, DirectFileInputModel.class),
                extractor(loader, DirectFileIoConstants.MODULE_NAME, DirectFileOutputModel.class));
        this.windgateJdbc = ExternalPortMap.collect(inputs, outputs,
                port -> Optionals.of(port.getInfo())
                    .flatMap(info -> WindGateJdbcIoAnalyzer.analyze(loader, info))
                    .filter(windgateJdbcFilter),
                port -> Optionals.of(port.getInfo())
                    .flatMap(info -> WindGateJdbcIoAnalyzer.analyze(loader, info))
                    .filter(windgateJdbcFilter));
        this.internal = ExternalPortMap.collect(inputs, outputs,
                extractor(loader, InternalIoConstants.MODULE_NAME, String.class),
                extractor(loader, InternalIoConstants.MODULE_NAME, String.class));
        this.generic = ExternalPortMap.select(inputs, outputs,
                p -> (directFile.contains(p) || windgateJdbc.contains(p) || internal.contains(p)) == false,
                p -> (directFile.contains(p) || windgateJdbc.contains(p) || internal.contains(p)) == false);
    }

    private static Predicate<WindGateJdbcModel> buildWindGateJdbcFilter(CompilerOptions options) {
        NamePattern pattern = WindGateJdbcIoAnalyzer.getProfileNamePattern(options);
        LOG.debug("WindGate JDBC direct profiles: {}", pattern);
        return model -> pattern.test(model.getProfileName());
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
     * Returns the input map.
     * @return the input map
     */
    public Map<SubPlan, ExternalInput> getInputMap() {
        return inputMap;
    }

    /**
     * Returns the output map.
     * @return the output map
     */
    public Map<SubPlan, ExternalOutput> getOutputMap() {
        return outputMap;
    }

    /**
     * Returns the generic I/O port map.
     * @return I/O port map
     */
    public ExternalPortMap<?, ?> getGeneric() {
        return generic;
    }

    /**
     * Returns the direct file I/O port map.
     * @return I/O port map
     */
    public ExternalPortMap<DirectFileInputModel, DirectFileOutputModel> getDirectFile() {
        return directFile;
    }

    /**
     * Returns the WindGate JDBC I/O port map.
     * @return the WindGate JDBC I/O port map
     */
    public ExternalPortMap<WindGateJdbcInputModel, WindGateJdbcOutputModel> getWindGateJdbc() {
        return windgateJdbc;
    }

    /**
     * Returns the internal I/O port map.
     * @return I/O port map
     */
    public ExternalPortMap<String, String> getInternal() {
        return internal;
    }

    private static <T extends Operator> Map<SubPlan, T> collectUnique(Plan plan, Class<T> type) {
        Map<SubPlan, T> results = new LinkedHashMap<>();
        for (SubPlan sub : plan.getElements()) {
            List<Operator> candidates = sub.getOperators().stream()
                .filter(type::isInstance)
                .collect(Collectors.toList());
            Invariants.require(candidates.size() <= 1, () -> candidates);
            if (candidates.size() == 1) {
                results.put(sub, type.cast(candidates.iterator().next()));
            }
        }
        return results;
    }

    private static <P extends ExternalPort> Set<P> collect(Plan plan, Class<P> portType) {
        return plan.getElements().stream()
                .flatMap(s -> s.getOperators().stream())
                .filter(portType::isInstance)
                .map(portType::cast)
                .filter(ExternalPort::isExternal)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T> Function<ExternalPort, Optional<T>> extractor(
            ClassLoader classLoader, String moduleName, Class<T> modelType) {
        return port -> {
            ExternalPortInfo info = port.getInfo();
            if (info != null && info.getModuleName().equals(moduleName)) {
                ValueDescription contents = info.getContents();
                if (contents != null) {
                    try {
                        Object model = contents.resolve(classLoader);
                        if (modelType.isInstance(model)) {
                            return Optionals.of(modelType.cast(model));
                        }
                    } catch (ReflectiveOperationException e) {
                        LOG.warn(MessageFormat.format(
                                "failed to resolve external port: {0} ({1})",
                                port, info.getModuleName()), e);
                    }
                }
            }
            return Optionals.empty();
        };
    }
}
