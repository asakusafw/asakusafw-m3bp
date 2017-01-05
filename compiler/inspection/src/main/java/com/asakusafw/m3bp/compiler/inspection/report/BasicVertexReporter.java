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
package com.asakusafw.m3bp.compiler.inspection.report;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Invariants;
import com.asakusafw.m3bp.compiler.inspection.EdgeResolver;
import com.asakusafw.m3bp.compiler.inspection.InputSpecView;
import com.asakusafw.m3bp.compiler.inspection.OutputSpecView;
import com.asakusafw.m3bp.compiler.inspection.PortSpecView;
import com.asakusafw.m3bp.compiler.inspection.VertexSpecView;

/**
 * A basic implementation of {@link VertexReporter}.
 */
public class BasicVertexReporter implements VertexReporter {

    private static final SectionReporter[] DEFAULT_SECTIONS = new SectionReporter[] {
            (w, v, e) -> {
                // core
                w.append(String.format("id: %s", v.getId()));
                w.append(String.format("operation: %s", v.getOperationType()));
                w.append(String.format("primary: %s", v.getPrimaryOperator()));
                w.append(String.format("options: %s", v.getOperationOptions()));
                w.append(String.format("inspection: %s [%s]", v.getOrigin().getId(), v.getOrigin().getTitle()));
            },
            (w, v, e) -> {
                // properties
                reportProperties(w, v.getAttributes());
            },
            (w, v, e) -> {
                // inputs
                for (InputSpecView port : v.getInputs()) {
                    reportInput(w, port, e.getOpposites(port));
                }
            },
            (w, v, e) -> {
                // outputs
                for (OutputSpecView port : v.getOutputs()) {
                    reportOutput(w, port, e.getOpposites(port));
                }
            },
    };

    private final Appender writer;

    private final List<SectionReporter> sections;

    private final boolean close;

    /**
     * Creates a new instance.
     * @param target the report output
     * @param close {@code true} to close the output on closing this reporter, otherwise {@code false}
     * @param extras the extra section reporters
     */
    public BasicVertexReporter(Appendable target, boolean close, SectionReporter... extras) {
        Arguments.requireNonNull(target);
        this.writer = new Appender(target);
        this.close = close;
        this.sections = Stream.concat(Stream.of(DEFAULT_SECTIONS), Stream.of(extras)).collect(Collectors.toList());
    }

    @Override
    public void report(VertexSpecView vertex, EdgeResolver edges) throws IOException {
        writer.block(String.format("vertex: %s (%s)",
                vertex.getId(),
                vertex.getPrimaryOperator()), () -> {
                    for (SectionReporter section : sections) {
                        section.report(writer, vertex, edges);
                    }
                });
    }

    @Override
    public void close() throws IOException {
        Appendable target = writer.target;
        if (close && target instanceof Closeable) {
            ((Closeable) target).close();
        }
    }

    private static void reportInput(
            ReportWriter writer, InputSpecView port,
            Set<? extends PortSpecView> opposites) throws IOException {
        writer.block(String.format("input: %s (%s)", port.getId(), port.getInputType()), () -> {
            writer.append(String.format("id: %s", port.getPortId()));
            writer.append(String.format("operation: %s", port.getInputType()));
            writer.append(String.format("data type: %s", port.getDataType()));
            writer.append(String.format("options: %s", port.getInputOptions()));
            writer.append(String.format("partition: %s", port.getPartitionInfo()));
            writer.append(String.format("inspection: %s", port.getOrigin().getId()));
            reportProperties(writer, port.getAttributes());
            reportOpposites(writer, "<- ", opposites);
        });
    }

    private static void reportOutput(
            ReportWriter writer, OutputSpecView port,
            Set<? extends PortSpecView> opposites) throws IOException {
        writer.block(String.format("output: %s (%s)", port.getId(), port.getOutputType()), () -> {
            writer.append(String.format("id: %s", port.getPortId()));
            writer.append(String.format("operation: %s", port.getOutputType()));
            writer.append(String.format("destination type: %s", port.getDataType()));
            writer.append(String.format("options: %s", port.getOutputOptions()));
            writer.append(String.format("partition: %s", port.getPartitionInfo()));
            writer.append(String.format("aggregation: %s", port.getAggregationInfo()));
            writer.append(String.format("inspection: %s", port.getOrigin().getId()));
            reportProperties(writer, port.getAttributes());
            reportOpposites(writer, "-> ", opposites);
        });
    }

    private static void reportOpposites(
            ReportWriter writer,
            String header, Set<? extends PortSpecView> opposites) throws IOException {
        if (opposites.isEmpty()) {
            writer.append("opposites: N/A");
        } else {
            writer.block("opposites:", () -> {
                for (PortSpecView opposite : opposites) {
                    writer.append(header + opposite.getId());
                }
            });
        }
    }

    private static void reportProperties(ReportWriter writer, Map<String, String> properties) throws IOException {
        if (properties.isEmpty()) {
            writer.append("properties: N/A");
        } else {
            writer.block("properties:", () -> {
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    writer.append(String.format("%s: %s", entry.getKey(), entry.getValue()));
                }
            });
        }
    }

    private static final class Appender implements ReportWriter {

        private static final String LINE_BREAK = String.format("%n");

        final Appendable target;

        private int indentLevel;

        Appender(Appendable target) {
            this.target = target;
        }

        @Override
        public Appender enter() {
            indentLevel++;
            return this;
        }

        @Override
        public Appender exit() {
            Invariants.require(indentLevel > 0);
            indentLevel--;
            return this;
        }

        @Override
        public Appender append(String line) throws IOException {
            target.append(indent(new StringBuilder())
                    .append(line)
                    .append(LINE_BREAK));
            return this;
        }

        private StringBuilder indent(StringBuilder buf) {
            for (int i = 0; i < indentLevel; i++) {
                buf.append(' ');
                buf.append(' ');
            }
            return buf;
        }
    }
}
