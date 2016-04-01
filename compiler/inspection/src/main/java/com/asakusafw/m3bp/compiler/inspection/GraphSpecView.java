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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.dag.utils.common.Tuple;
import com.asakusafw.lang.inspection.InspectionNode;
import com.asakusafw.lang.inspection.InspectionNode.PortReference;

/**
 * Represents graphs.
 */
public class GraphSpecView implements ElementSpecView<InspectionNode>, EdgeResolver {

    static final String VALUE_KIND = "Plan";

    private final InspectionNode origin;

    private final String id;

    private final List<VertexSpecView> vertices;

    private final Map<PortReference, InputSpecView> inputs;

    private final Map<PortReference, OutputSpecView> outputs;

    private final Map<String, String> properties;

    /**
     * Creates a new instance.
     * @param origin the original element
     * @param id the graph ID
     * @param vertices the vertices
     * @param properties the properties
     */
    public GraphSpecView(
            InspectionNode origin,
            String id,
            Collection<VertexSpecView> vertices,
            Map<String, String> properties) {
        Arguments.requireNonNull(origin);
        Arguments.requireNonNull(id);
        Arguments.requireNonNull(vertices);
        Arguments.requireNonNull(properties);
        this.origin = origin;
        this.id = id;
        this.vertices = Arguments.freezeToList(vertices);
        this.inputs = vertices.stream()
                .flatMap(v -> v.getInputs().stream().map(p -> new Tuple<>(v, p)))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                t -> new PortReference(t.left().getOrigin().getId(), t.right().getOrigin().getId()),
                                Tuple::right),
                        Collections::unmodifiableMap));
        this.outputs = vertices.stream()
                .flatMap(v -> v.getOutputs().stream().map(p -> new Tuple<>(v, p)))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                t -> new PortReference(t.left().getOrigin().getId(), t.right().getOrigin().getId()),
                                Tuple::right),
                        Collections::unmodifiableMap));
        this.properties = Arguments.freeze(properties);
    }

    /**
     * Returns whether the target node represents a graph.
     * @param node the target node
     * @return {@code true} if the target node represents a graph, otherwise {@code false}
     */
    public static boolean isGraph(InspectionNode node) {
        return Optionals.get(node.getProperties(), KEY_KIND)
                .filter(VALUE_KIND::equals)
                .isPresent();
    }

    /**
     * Parses the {@link InspectionNode} and returns the related {@link GraphSpecView}.
     * @param node the target node
     * @return the parsed object, or {@code null} if the target node does not represent a valid graph
     */
    public static GraphSpecView parse(InspectionNode node) {
        Arguments.requireNonNull(node);
        Collection<InspectionNode> elements = node.getElements().values();
        if (isGraph(node) == false) {
            return null;
        }
        AttributeExtractor extractor = new AttributeExtractor(node.getProperties());
        return new GraphSpecView(
                node,
                node.getId(),
                elements.stream()
                    .map(VertexSpecView::parse)
                    .sorted((a, b) -> a.getId().compareTo(b.getId()))
                    .collect(Collectors.toList()),
                extractor.extractProperties());
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
     * Returns the vertices.
     * @return the vertices
     */
    public List<VertexSpecView> getVertices() {
        return vertices;
    }

    @Override
    public Set<OutputSpecView> getOpposites(InputSpecView port) {
        Arguments.requireNonNull(port);
        return port.getOrigin().getOpposites().stream()
                .map(outputs::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<InputSpecView> getOpposites(OutputSpecView port) {
        Arguments.requireNonNull(port);
        return port.getOrigin().getOpposites().stream()
                .map(inputs::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Map<String, String> getAttributes() {
        return properties;
    }
}
