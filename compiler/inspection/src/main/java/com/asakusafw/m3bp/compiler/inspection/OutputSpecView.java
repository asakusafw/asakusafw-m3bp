/**
 * Copyright 2011-2017 Asakusa Framework Team.
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

import com.asakusafw.lang.inspection.InspectionNode;
import com.asakusafw.lang.utils.common.Arguments;

/**
 * Represents output ports.
 */
public class OutputSpecView implements PortSpecView {

    static final String KEY_PREFIX = KEY_ATTRIBUTE_PREFIX + "OutputSpec.";

    static final String KEY_ID = KEY_PREFIX + "id";

    static final String KEY_OUTPUT_TYPE = KEY_PREFIX + "type";

    static final String KEY_DATA_TYPE = KEY_PREFIX + "data";

    static final String KEY_OUTPUT_OPTIONS = KEY_PREFIX + "options";

    static final String KEY_PARTITION_INFO = KEY_PREFIX + "partition";

    static final String KEY_AGGREGATION_INFO = KEY_PREFIX + "aggregation";

    private final InspectionNode.Port origin;

    private final String vertexId;

    private final String portId;

    private final String outputType;

    private final String dataType;

    private final Set<String> outputOptions;

    private final String partitionInfo;

    private final String aggregationInfo;

    private final Map<String, String> properties;

    /**
     * Creates a new instance.
     * @param origin the original node
     * @param vertexId the vertex ID
     * @param portId the port ID
     * @param outputType the output type
     * @param dataType the output data type
     * @param outputOptions the output options
     * @param partitionInfo the partitioning information
     * @param aggregationInfo the pre-aggregation operator information
     * @param properties the extra properties
     */
    public OutputSpecView(
            InspectionNode.Port origin,
            String vertexId,
            String portId,
            String outputType,
            String dataType,
            Set<String> outputOptions,
            String partitionInfo,
            String aggregationInfo,
            Map<String, String> properties) {
        Arguments.requireNonNull(origin);
        Arguments.requireNonNull(vertexId);
        Arguments.requireNonNull(portId);
        Arguments.requireNonNull(properties);
        this.origin = origin;
        this.vertexId = vertexId;
        this.portId = portId;
        this.outputType = outputType;
        this.dataType = dataType;
        this.outputOptions = outputOptions;
        this.partitionInfo = partitionInfo;
        this.aggregationInfo = aggregationInfo;
        this.properties = properties;
    }

    /**
     * Parses the port of {@link InspectionNode} and returns the related {@link OutputSpecView}.
     * @param port the target node
     * @param vertexId the parent ID
     * @return the parsed object
     */
    public static OutputSpecView parse(InspectionNode.Port port, String vertexId) {
        Arguments.requireNonNull(port);
        Arguments.requireNonNull(vertexId);
        AttributeExtractor extractor = new AttributeExtractor(port.getProperties());
        String portId = extractor.extract(KEY_ID).orElse("!" + port.getId());
        return new OutputSpecView(
                port, vertexId, portId,
                extractor.extract(KEY_OUTPUT_TYPE).orElse("N/A"),
                extractor.extract(KEY_DATA_TYPE).orElse("N/A"),
                extractor.extractSet(KEY_OUTPUT_OPTIONS).orElseGet(() -> Collections.singleton("N/A")),
                extractor.extract(KEY_PARTITION_INFO).orElse("N/A"),
                extractor.extract(KEY_AGGREGATION_INFO).orElse("N/A"),
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
     * Returns the output type.
     * @return the output type
     */
    public String getOutputType() {
        return outputType;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns the output options.
     * @return the output options
     */
    public Set<String> getOutputOptions() {
        return outputOptions;
    }

    /**
     * Returns the partitioning info.
     * @return the partitioning info
     */
    public String getPartitionInfo() {
        return partitionInfo;
    }

    /**
     * Returns the pre-aggregation operator info.
     * @return the pre-aggregation operator info
     */
    public String getAggregationInfo() {
        return aggregationInfo;
    }


    @Override
    public Map<String, String> getAttributes() {
        return properties;
    }
}
