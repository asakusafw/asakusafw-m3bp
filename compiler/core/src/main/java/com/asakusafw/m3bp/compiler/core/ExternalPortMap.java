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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.dag.utils.common.Tuple;
import com.asakusafw.lang.compiler.model.graph.ExternalInput;
import com.asakusafw.lang.compiler.model.graph.ExternalOutput;

/**
 * External I/O map.
 * @param <TInput> the input model type
 * @param <TOutput> the output model type
 * @since 0.2.0
 */
public final class ExternalPortMap<TInput, TOutput> {

    private final Map<ExternalInput, TInput> inputs;

    private final Map<ExternalOutput, TOutput> outputs;

    private ExternalPortMap(Map<ExternalInput, TInput> inputs, Map<ExternalOutput, TOutput> outputs) {
        this.inputs = Collections.unmodifiableMap(inputs);
        this.outputs = Collections.unmodifiableMap(outputs);
    }

    static <TInput, TOutput> ExternalPortMap<TInput, TOutput> collect(
            Collection<ExternalInput> in,
            Collection<ExternalOutput> out,
            Function<? super ExternalInput, ? extends Optional<? extends TInput>> inputResolver,
            Function<? super ExternalOutput, ? extends Optional<? extends TOutput>> outputResolver) {
        return new ExternalPortMap<>(
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

    static ExternalPortMap<?, ?> select(
            Collection<ExternalInput> in,
            Collection<ExternalOutput> out,
            Predicate<? super ExternalInput> inputFilter,
            Predicate<? super ExternalOutput> outputFilter) {
        return new ExternalPortMap<>(
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