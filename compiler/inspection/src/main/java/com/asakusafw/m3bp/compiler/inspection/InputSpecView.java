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
import java.util.Map;
import java.util.Set;

import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.lang.inspection.InspectionNode;

/**
 * Represents input ports.
 */
public class InputSpecView implements PortSpecView {

    static final String KEY_PREFIX = KEY_ATTRIBUTE_PREFIX + "InputSpec.";

    static final String KEY_ID = KEY_PREFIX + "id";

    static final String KEY_INPUT_TYPE = KEY_PREFIX + "type";

    static final String KEY_DATA_TYPE = KEY_PREFIX + "data";

    static final String KEY_INPUT_OPTIONS = KEY_PREFIX + "options";

    static final String KEY_PARTITION_INFO = KEY_PREFIX + "partition";

    private final InspectionNode.Port origin;

    private final String vertexId;

    private final String portId;

    private final String inputType;

    private final String dataType;

    private final Set<String> inputOptions;

    private final String partitionInfo;

    private final Map<String, String> properties;

    /**
     * Creates a new instance.
     * @param origin the original node
     * @param vertexId the vertex ID
     * @param portId the port ID
     * @param inputType the input type
     * @param dataType the input data type
     * @param inputOptions the input options
     * @param partitionInfo the partitioning information
     * @param properties the extra properties
     */
    public InputSpecView(
            InspectionNode.Port origin,
            String vertexId,
            String portId,
            String inputType,
            String dataType,
            Set<String> inputOptions,
            String partitionInfo,
            Map<String, String> properties) {
        Arguments.requireNonNull(origin);
        Arguments.requireNonNull(vertexId);
        Arguments.requireNonNull(portId);
        Arguments.requireNonNull(properties);
        this.origin = origin;
        this.vertexId = vertexId;
        this.portId = portId;
        this.inputType = inputType;
        this.dataType = dataType;
        this.inputOptions = inputOptions;
        this.partitionInfo = partitionInfo;
        this.properties = properties;
    }

    /**
     * Parses the port of {@link InspectionNode} and returns the related {@link InputSpecView}.
     * @param port the target node
     * @param vertexId the parent ID
     * @return the parsed object
     */
    public static InputSpecView parse(InspectionNode.Port port, String vertexId) {
        Arguments.requireNonNull(port);
        Arguments.requireNonNull(vertexId);
        AttributeExtractor extractor = new AttributeExtractor(port.getProperties());
        String portId = extractor.extract(KEY_ID).orElse("!" + port.getId());
        return new InputSpecView(
                port, vertexId, portId,
                extractor.extract(KEY_INPUT_TYPE).orElse("N/A"),
                extractor.extract(KEY_DATA_TYPE).orElse("N/A"),
                extractor.extractSet(KEY_INPUT_OPTIONS).orElseGet(() -> Collections.singleton("N/A")),
                extractor.extract(KEY_PARTITION_INFO).orElse("N/A"),
                extractor.extractProperties());
    }

    @Override
    public InspectionNode.Port getOrigin() {
        return origin;
    }

    @Override
    public String getVertexId() {
        return vertexId;
    }

    @Override
    public String getPortId() {
        return portId;
    }

    /**
     * Returns the input type.
     * @return the input type
     */
    public String getInputType() {
        return inputType;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns the partition info.
     * @return the partition info
     */
    public String getPartitionInfo() {
        return partitionInfo;
    }

    /**
     * Returns the input options.
     * @return the input options
     */
    public Set<String> getInputOptions() {
        return inputOptions;
    }

    @Override
    public Map<String, String> getAttributes() {
        return properties;
    }
}
