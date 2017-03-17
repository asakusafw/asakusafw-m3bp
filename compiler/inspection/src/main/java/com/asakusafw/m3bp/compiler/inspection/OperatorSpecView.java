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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.asakusafw.lang.inspection.InspectionNode;
import com.asakusafw.lang.utils.common.Optionals;

/**
 * Represents operators.
 * @since 0.2.1
 */
public class OperatorSpecView implements ElementSpecView<InspectionNode> {

    static final String PROPERTY_KIND = "kind"; //$NON-NLS-1$

    static final String PROPERTY_NAME = "name"; //$NON-NLS-1$

    static final String PROPERTY_SERIAL_NUMBER = "originalSerialNumber";

    static final String PROPERTY_ARRGUMENT_PREFIX = "arguments.";

    static final String PROPERTY_CLASS = "class";

    static final String PROPERTY_METHOD = "method";

    static final String PROPERTY_TYPE = "type";

    static final String PROPERTY_DESCRIPTION = "description";

    static final String KIND_INPUT = "ExternalInput";

    static final String KIND_OUTPUT = "ExternalOutput";

    static final String KIND_MARKER = "MarkerOperator";

    static final Pattern PATTERN_TYPE = Pattern.compile("Class\\((.*?)\\)");

    private final InspectionNode origin;

    private final String id;

    private final OperatorKind operatorKind;

    private final int serialNumber;

    private final String title;

    private final Map<String, String> properties;

    /**
     * Creates a new instance.
     * @param origin the original node
     * @param id the operator ID
     * @param operatorKind the operator kind
     * @param serialNumber the serial number
     * @param title the operator title
     * @param properties the properties
     */
    public OperatorSpecView(
            InspectionNode origin,
            String id,
            OperatorKind operatorKind,
            int serialNumber,
            String title,
            Map<String, String> properties) {
        this.origin = origin;
        this.id = id;
        this.operatorKind = operatorKind;
        this.serialNumber = serialNumber;
        this.title = title;
        this.properties = properties;
    }

    /**
     * Parses the {@link InspectionNode} and returns the related {@link OperatorSpecView}.
     * @param node the target node
     * @return the parsed object
     */
    public static OperatorSpecView parse(InspectionNode node) {
        OperatorKind kind = OperatorKind.detect(node);
        Map<String, String> props = node.getProperties();
        return new OperatorSpecView(
                node, node.getId(),
                kind,
                Optionals.get(props, PROPERTY_SERIAL_NUMBER)
                        .map(Integer::valueOf)
                        .orElse(-1),
                toTitle(node, kind),
                new AttributeExtractor(props).extractProperties());
    }

    private static String toTitle(InspectionNode node, OperatorKind kind) {
        Map<String, String> props = node.getProperties();
        switch (kind) {
        case EXTERNAL_INPUT:
            return String.format("@Import[%s](%s:%s)",
                    toTypes(node.getOutputs()),
                    Optionals.get(props, PROPERTY_NAME).orElse("N/A"),
                    Optionals.get(props, PROPERTY_DESCRIPTION).map(OperatorSpecView::toSimpleName).orElse("N/A"));
        case EXTERNAL_OUTPUT:
            return String.format("@Export[%s](%s:%s)",
                    toTypes(node.getInputs()),
                    Optionals.get(props, PROPERTY_NAME).orElse("N/A"),
                    Optionals.get(props, PROPERTY_DESCRIPTION).map(OperatorSpecView::toSimpleName).orElse("N/A"));
        case CORE:
            return toCoreTitle(node);
        case USER:
            return toUserTitle(node);
        case MARKER:
            return "(Marker)";
        case UNKNOWN:
            return "(Unknown)";
        default:
            throw new AssertionError(kind);
        }
    }

    private static String toCoreTitle(InspectionNode node) {
        Map<String, String> props = node.getProperties();
        return String.format("@%s[%s -> %s]",
                Optionals.get(props, PROPERTY_KIND).orElse("N/A"),
                toTypes(node.getInputs()),
                toTypes(node.getOutputs()));
    }

    private static String toUserTitle(InspectionNode node) {
        Map<String, String> props = node.getProperties();
        return String.format("@%s:%s.%s[%s -> %s](%s)",
                Optionals.get(props, PROPERTY_KIND).orElse("N/A"),
                Optionals.get(props, PROPERTY_CLASS).map(OperatorSpecView::toSimpleName).orElse("N/A"),
                Optionals.get(props, PROPERTY_METHOD).orElse("N/A"),
                toTypes(node.getInputs()),
                toTypes(node.getOutputs()),
                props.entrySet().stream()
                    .sequential()
                    .filter(e -> e.getKey().startsWith(PROPERTY_ARRGUMENT_PREFIX))
                    .map(e -> String.format("%s:%s",
                            e.getKey().substring(PROPERTY_ARRGUMENT_PREFIX.length()),
                            e.getValue()))
                    .collect(Collectors.joining(", ")));
    }

    private static String toSimpleName(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index >= name.length() - 1) {
            return name;
        } else {
            return name.substring(index + 1);
        }
    }

    private static String toTypes(Map<String, InspectionNode.Port> ports) {
        return ports.values().stream()
                .sequential()
                .map(p -> Optionals.get(p.getProperties(), PROPERTY_TYPE)
                        .flatMap(s -> {
                            Matcher matcher = PATTERN_TYPE.matcher(s);
                            if (matcher.matches()) {
                                return Optionals.of(matcher.group(1));
                            } else {
                                return Optionals.empty();
                            }
                        })
                        .map(OperatorSpecView::toSimpleName)
                        .orElse("N/A"))
                .collect(Collectors.joining(", "));
    }

    @Override
    public InspectionNode getOrigin() {
        return origin;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, String> getAttributes() {
        return properties;
    }

    /**
     * Returns the operator kind.
     * @return the operator kind
     */
    public OperatorKind getOperatorKind() {
        return operatorKind;
    }

    /**
     * Returns the operator serial number.
     * @return the operator serial number
     */
    public int getSerialNumber() {
        return serialNumber;
    }

    /**
     * Returns the operator title.
     * @return the operator title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Represents operator kind.
     * @since 0.2.1
     */
    public enum OperatorKind {

        /**
         * The external input.
         */
        EXTERNAL_INPUT(0),

        /**
         * The external output.
         */
        EXTERNAL_OUTPUT(3),

        /**
         * The marker operator.
         */
        MARKER(2),

        /**
         * The core operator.
         */
        CORE(1),

        /**
         * The user operator.
         */
        USER(1),

        /**
         * The unknown operator.
         */
        UNKNOWN(4),
        ;

        private int printOrder;

        OperatorKind(int printOrder) {
            this.printOrder = printOrder;
        }

        int getPrintOrder() {
            return printOrder;
        }

        static OperatorKind detect(InspectionNode node) {
            Map<String, String> props = node.getProperties();
            String kindString = props.get(PROPERTY_KIND);
            if (kindString == null) {
                return UNKNOWN;
            }
            switch (kindString) {
            case KIND_INPUT:
                return EXTERNAL_INPUT;
            case KIND_OUTPUT:
                return EXTERNAL_OUTPUT;
            case KIND_MARKER:
                return MARKER;
            default:
                break;
            }
            return Optionals.get(props, PROPERTY_METHOD)
                    .map(s -> USER)
                    .orElse(CORE);
        }
    }
}
