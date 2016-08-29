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
import java.util.NoSuchElementException;
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
import com.asakusafw.dag.utils.common.Tuple;
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

    private final IoMap<DirectFileInputModel, DirectFileOutputModel> directFile;

    private final IoMap<WindGateJdbcInputModel, WindGateJdbcOutputModel> windgateJdbc;

    private final IoMap<String, String> internal;

    private final IoMap<?, ?> generic;

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

        Set<ExternalInput> inputs = collect(loader, plan, ExternalInput.class);
        Set<ExternalOutput> outputs = collect(loader, plan, ExternalOutput.class);
        Predicate<WindGateJdbcModel> windgateJdbcFilter = buildWindGateJdbcFilter(options);
        this.directFile = IoMap.collect(inputs, outputs,
                extractor(loader, DirectFileIoConstants.MODULE_NAME, DirectFileInputModel.class),
                extractor(loader, DirectFileIoConstants.MODULE_NAME, DirectFileOutputModel.class));
        this.windgateJdbc = IoMap.collect(inputs, outputs,
                port -> Optionals.of(port.getInfo())
                    .flatMap(info -> WindGateJdbcIoAnalyzer.analyze(loader, info))
                    .filter(windgateJdbcFilter),
                port -> Optionals.of(port.getInfo())
                    .flatMap(info -> WindGateJdbcIoAnalyzer.analyze(loader, info))
                    .filter(windgateJdbcFilter));
        this.internal = IoMap.collect(inputs, outputs,
                extractor(loader, InternalIoConstants.MODULE_NAME, String.class),
                extractor(loader, InternalIoConstants.MODULE_NAME, String.class));
        this.generic = IoMap.select(inputs, outputs,
                p -> (directFile.contains(p) || windgateJdbc.contains(p) || internal.contains(p)) == false,
                p -> (directFile.contains(p) || windgateJdbc.contains(p) || internal.contains(p)) == false);
    }

    private Predicate<WindGateJdbcModel> buildWindGateJdbcFilter(CompilerOptions options) {
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
    public IoMap<?, ?> getGeneric() {
        return generic;
    }

    /**
     * Returns the direct file I/O port map.
     * @return I/O port map
     */
    public IoMap<DirectFileInputModel, DirectFileOutputModel> getDirectFile() {
        return directFile;
    }

    /**
     * Returns the WindGate JDBC I/O port map.
     * @return the WindGate JDBC I/O port map
     */
    public IoMap<WindGateJdbcInputModel, WindGateJdbcOutputModel> getWindGateJdbc() {
        return windgateJdbc;
    }

    /**
     * Returns the internal I/O port map.
     * @return I/O port map
     */
    public IoMap<String, String> getInternal() {
        return internal;
    }

    private <T extends Operator> Map<SubPlan, T> collectUnique(Plan plan, Class<T> type) {
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

    private <P extends ExternalPort> Set<P> collect(ClassLoader loader, Plan plan, Class<P> portType) {
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

    /**
     * External I/O map.
     * @param <TInput> the input model type
     * @param <TOutput> the output model type
     * @since 0.2.0
     */
    public static final class IoMap<TInput, TOutput> {

        private final Map<ExternalInput, TInput> inputs;

        private final Map<ExternalOutput, TOutput> outputs;

        private IoMap(Map<ExternalInput, TInput> inputs, Map<ExternalOutput, TOutput> outputs) {
            this.inputs = Collections.unmodifiableMap(inputs);
            this.outputs = Collections.unmodifiableMap(outputs);
        }

        static <TInput, TOutput> IoMap<TInput, TOutput> collect(
                Collection<ExternalInput> in,
                Collection<ExternalOutput> out,
                Function<? super ExternalInput, ? extends Optional<? extends TInput>> inputResolver,
                Function<? super ExternalOutput, ? extends Optional<? extends TOutput>> outputResolver) {
            return new IoMap<>(
                    in.stream()
                        .map(p -> new Tuple<>(p, inputResolver.apply(p)))
                        .filter(t -> t.right().isPresent())
                        .map(t -> new Tuple<>(t.left(), t.right().get()))
                        .collect(Collectors.toMap(Tuple::left, Tuple::right)),
                    out.stream()
                        .map(p -> new Tuple<>(p, outputResolver.apply(p)))
                        .filter(t -> t.right().isPresent())
                        .map(t -> new Tuple<>(t.left(), t.right().get()))
                        .collect(Collectors.toMap(Tuple::left, Tuple::right)));
        }

        static IoMap<?, ?> select(
                Collection<ExternalInput> in,
                Collection<ExternalOutput> out,
                Predicate<? super ExternalInput> inputFilter,
                Predicate<? super ExternalOutput> outputFilter) {
            return new IoMap<>(
                    in.stream()
                        .filter(inputFilter)
                        .collect(Collectors.toMap(Function.identity(), Function.identity())),
                    out.stream()
                        .filter(outputFilter)
                        .collect(Collectors.toMap(Function.identity(), Function.identity())));
        }

        /**
         * Returns whether or not this map contains the target input.
         * @param port the target port
         * @return {@code true} if this map contains the target port, otherwise {@code false}
         */
        public boolean contains(ExternalInput port) {
            return inputs.containsKey(port);
        }

        /**
         * Returns whether or not this map contains the target output.
         * @param port the target port
         * @return {@code true} if this map contains the target port, otherwise {@code false}
         */
        public boolean contains(ExternalOutput port) {
            return outputs.containsKey(port);
        }

        /**
         * Returns the input ports in this map.
         * @return the input ports
         */
        public Set<ExternalInput> inputs() {
            return inputs.keySet();
        }

        /**
         * Returns the output ports in this map.
         * @return the output ports
         */
        public Set<ExternalOutput> outputs() {
            return outputs.keySet();
        }

        /**
         * Returns the model object for the target input.
         * @param port the target port.
         * @return the related model
         * @throws NoSuchElementException if it does not exist
         */
        public TInput get(ExternalInput port) {
            return Optionals.get(inputs, port)
                    .orElseThrow(() -> new NoSuchElementException(port.toString()));
        }

        /**
         * Returns the model object for the target output.
         * @param port the target port.
         * @return the related model
         * @throws NoSuchElementException if it does not exist
         */
        public TOutput get(ExternalOutput port) {
            return Optionals.get(outputs, port)
                    .orElseThrow(() -> new NoSuchElementException(port.toString()));
        }
    }
}
