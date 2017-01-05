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

import com.asakusafw.lang.inspection.InspectionNode;

/**
 * An abstract super interface of port specs.
 */
public interface PortSpecView extends ElementSpecView<InspectionNode.Port> {

    @Override
    default String getId() {
        return String.format("%s.%s", getVertexId(), getPortId());
    }

    /**
     * Returns the vertex ID.
     * @return the vertex ID
     */
    String getVertexId();

    /**
     * Returns the port ID.
     * @return the port ID
     */
    String getPortId();

    /**
     * Returns the data type.
     * @return the data type
     */
    String getDataType();
}