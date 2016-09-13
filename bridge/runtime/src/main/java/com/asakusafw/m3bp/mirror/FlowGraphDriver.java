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
package com.asakusafw.m3bp.mirror;

import static java.util.stream.Collectors.*;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.asakusafw.dag.api.model.EdgeInfo;
import com.asakusafw.dag.api.model.GraphInfo;
import com.asakusafw.dag.api.model.PortId;
import com.asakusafw.dag.api.model.PortInfo;
import com.asakusafw.dag.api.model.VertexInfo;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.descriptor.M3bpVertexDescriptor;

final class FlowGraphDriver {

    private FlowGraphDriver() {
        return;
    }

    static void drive(FlowGraphMirror mirror, GraphInfo info) {
        Map<VertexInfo, VertexMirror> vertices = driveVertices(mirror, info.getVertices());
        Map<PortId, PortMirror> inputs = driveInputs(vertices, info.getEdges());
        Map<PortId, PortMirror> outputs = driveOutputs(vertices, info.getEdges());
        driveEdges(mirror, info.getEdges(), inputs, outputs);
    }

    private static Map<VertexInfo, VertexMirror> driveVertices(FlowGraphMirror mirror, List<VertexInfo> vertices) {
        return vertices.stream().collect(Collectors.toMap(
                Function.identity(),
                v -> mirror.addVertex(v.getName(), (M3bpVertexDescriptor) v.getDescriptor())));
    }

    private static Map<PortId, PortMirror> driveInputs(Map<VertexInfo, VertexMirror> vertices, List<EdgeInfo> edges) {
        Map<PortId, Set<M3bpEdgeDescriptor>> descriptorMap = edges.stream().collect(groupingBy(
                EdgeInfo::getDownstreamId,
                mapping(e -> (M3bpEdgeDescriptor) e.getDescriptor(), toSet())));
        Map<PortId, PortMirror> results = new HashMap<>();
        vertices.forEach((k, v) -> {
            results.putAll(k.getInputPorts().stream().collect(toMap(
                    PortInfo::getId,
                    p -> {
                        Set<M3bpEdgeDescriptor> desc = descriptorMap.getOrDefault(p.getId(), Collections.emptySet());
                        Invariants.require(desc.size() == 1, () -> MessageFormat.format(
                                "ambiguous edges: {0} ({1})",
                                p.getId(), desc));
                        return v.addInput(p.getName(), p.getTag(), desc.stream().findFirst().get());
                    })));
        });
        return results;
    }

    private static Map<PortId, PortMirror> driveOutputs(Map<VertexInfo, VertexMirror> vertices, List<EdgeInfo> edges) {
        Map<PortId, Set<M3bpEdgeDescriptor>> descriptorMap = edges.stream().collect(groupingBy(
                EdgeInfo::getUpstreamId,
                mapping(e -> (M3bpEdgeDescriptor) e.getDescriptor(), toSet())));
        Map<PortId, PortMirror> results = new HashMap<>();
        vertices.forEach((k, v) -> {
            results.putAll(k.getOutputPorts().stream().collect(toMap(
                    PortInfo::getId,
                    p -> {
                        Set<M3bpEdgeDescriptor> desc = descriptorMap.getOrDefault(p.getId(), Collections.emptySet());
                        Invariants.require(desc.size() == 1, () -> MessageFormat.format(
                                "ambiguous edges: {0} ({1})",
                                p.getId(), desc));
                        return v.addOutput(p.getName(), p.getTag(), desc.stream().findFirst().get());
                    })));
        });
        return results;
    }

    private static void driveEdges(
            FlowGraphMirror mirror,
            List<EdgeInfo> edges,
            Map<PortId, PortMirror> inputs, Map<PortId, PortMirror> outputs) {
        edges.stream().forEach(e -> {
            PortMirror upstream = Optionals.get(outputs, e.getUpstreamId())
                    .orElseThrow(AssertionError::new);
            PortMirror downstream = Optionals.get(inputs, e.getDownstreamId())
                    .orElseThrow(AssertionError::new);
            mirror.addEdge(upstream, downstream);
        });
    }
}
