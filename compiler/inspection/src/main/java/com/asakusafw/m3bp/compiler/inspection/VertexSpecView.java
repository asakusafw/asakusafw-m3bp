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
package com.asakusafw.m3bp.compiler.inspection;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.lang.inspection.InspectionNode;

/**
 * Represents vertices.
 */
public class VertexSpecView implements ElementSpecView<InspectionNode> {

    static final String KEY_PREFIX = KEY_ATTRIBUTE_PREFIX + "VertexSpec.";

    static final String KEY_ID = KEY_PREFIX + "id";

    static final String KEY_OPERATION_TYPE = KEY_PREFIX + "type";

    static final String KEY_PRIMARY_OPERATOR = KEY_PREFIX + "primary";

    static final String KEY_OPERATION_OPTIONS = KEY_PREFIX + "options";

    static final String VALUE_KIND = "SubPlan";

    private final InspectionNode origin;

    private final String id;

    private final String operationType;

    private final String primaryOperator;

    private final Set<String> operationOptions;

    private final List<InputSpecView> inputs;

    private final List<OutputSpecView> outputs;

    private final Map<String, String> properties;

    /**
     * Creates a new instance.
     * @param origin the original node
     * @param id the vertex ID
     * @param operationType the operation type
     * @param primaryOperator the primary operator
     * @param operationOptions the operation options
     * @param inputs the input ports
     * @param outputs the output ports
     * @param properties the extra properties
     */
    public VertexSpecView(
            InspectionNode origin,
            String id,
            String operationType,
            String primaryOperator,
            Set<String> operationOptions,
            List<InputSpecView> inputs,
            List<OutputSpecView> outputs,
            Map<String, String> properties) {
        Arguments.requireNonNull(origin);
        Arguments.requireNonNull(id);
        Arguments.requireNonNull(inputs);
        Arguments.requireNonNull(outputs);
        Arguments.requireNonNull(properties);
        this.origin = origin;
        this.id = id;
        this.operationType = operationType;
        this.primaryOperator = primaryOperator;
        this.operationOptions = operationOptions;
        this.inputs = Arguments.freeze(inputs);
        this.outputs = Arguments.freeze(outputs);
        this.properties = Arguments.freeze(properties);
    }

    /**
     * Parses the {@link InspectionNode} and returns the related {@link VertexSpecView}.
     * @param node the target node
     * @return the parsed object
     */
    public static VertexSpecView parse(InspectionNode node) {
        Arguments.requireNonNull(node);
        AttributeExtractor extractor = new AttributeExtractor(node.getProperties());
        String id = extractor.extract(KEY_ID).orElse("!" + node.getId());
        return new VertexSpecView(
                node, id,
                extractor.extract(KEY_OPERATION_TYPE).orElse("N/A"),
                extractor.extract(KEY_PRIMARY_OPERATOR).orElse("N/A"),
                extractor.extractSet(KEY_OPERATION_OPTIONS).orElseGet(() -> Collections.singleton("N/A")),
                node.getInputs().values().stream()
                    .map(p -> InputSpecView.parse(p, id))
                    .sorted((a, b) -> a.getPortId().compareTo(b.getPortId()))
                    .collect(Collectors.toList()),
                node.getOutputs().values().stream()
                    .map(p -> OutputSpecView.parse(p, id))
                    .sorted((a, b) -> a.getPortId().compareTo(b.getPortId()))
                    .collect(Collectors.toList()),
                extractor.extractProperties());
    }

    /**
     * Returns whether the target represents a valid vertex or not.
     * @param node the target node
     * @return {@code true} if the target represents a valid vertex, otherwise {@code false}
     */
    public static boolean isVertex(InspectionNode node) {
        return node.getProperties().getOrDefault(KEY_KIND, "").equals(VALUE_KIND)
                && node.getProperties().containsKey(KEY_ID);
    }

    @Override
    public InspectionNode getOrigin() {
        return origin;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns the operation type.
     * @return the operation type
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * Returns the primary operator.
     * @return the primary operator
     */
    public String getPrimaryOperator() {
        return primaryOperator;
    }

    /**
     * Returns the operation options.
     * @return the operation options
     */
    public Set<String> getOperationOptions() {
        return operationOptions;
    }

    /**
     * Returns the input ports.
     * @return the input ports
     */
    public List<InputSpecView> getInputs() {
        return inputs;
    }

    /**
     * Returns the output ports.
     * @return the output ports
     */
    public List<OutputSpecView> getOutputs() {
        return outputs;
    }

    @Override
    public Map<String, String> getAttributes() {
        return properties;
    }
}
