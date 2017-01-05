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
package com.asakusafw.m3bp.compiler.inspection.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.lang.inspection.InspectionNode;
import com.asakusafw.lang.inspection.InspectionNodeRepository;
import com.asakusafw.lang.inspection.json.JsonInspectionNodeRepository;
import com.asakusafw.m3bp.compiler.common.M3bpPackage;
import com.asakusafw.m3bp.compiler.inspection.GraphSpecView;
import com.asakusafw.m3bp.compiler.inspection.VertexSpecView;
import com.asakusafw.m3bp.compiler.inspection.report.BasicVertexReporter;
import com.asakusafw.m3bp.compiler.inspection.report.VertexReporter;

/**
 * Processes inspection object.
 */
public final class Cli {

    static final Logger LOG = LoggerFactory.getLogger(Cli.class);

    private Cli() {
        return;
    }

    /**
     * The program entry.
     * @param args application arguments
     * @throws Exception if failed
     */
    public static void main(String[] args) throws Exception {
        int status = execute(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * The program entry.
     * @param args application arguments
     * @return the exit code
     */
    public static int execute(String... args) {
        Configuration configuration;
        try {
            configuration = parse(args);
        } catch (Exception e) {
            LOG.error(MessageFormat.format(
                    "error occurred while analyzing arguments: {0}",
                    Arrays.toString(args)), e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(Integer.MAX_VALUE);
            formatter.printHelp(
                    MessageFormat.format(
                            "java -classpath ... {0}", //$NON-NLS-1$
                            Cli.class.getName()),
                    new Opts().options,
                    true);
            return 1;
        }
        try {
            process(configuration);
        } catch (Exception e) {
            LOG.error(MessageFormat.format(
                    "error occurred while processing inspection object: {0}",
                    Arrays.toString(args)), e);
            return 1;
        }
        return 0;
    }

    static Configuration parse(String... args) throws ParseException {
        LOG.debug("analyzing command line arguments: {}", Arrays.toString(args)); //$NON-NLS-1$

        Opts opts = new Opts();
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(opts.options, args);

        Configuration results = new Configuration();
        results.input = parseFile(cmd, opts.input, true);
        results.vertexId = parseOpt(cmd, opts.vertex, false);
        return results;
    }

    private static File parseFile(CommandLine cmd, Option opt, boolean mandatory) {
        String value = parseOpt(cmd, opt, mandatory);
        if (value == null) {
            return null;
        }
        return new File(value);
    }

    private static String parseOpt(CommandLine cmd, Option opt, boolean mandatory) {
        String value = cmd.getOptionValue(opt.getLongOpt());
        if (value != null) {
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            }
        }
        LOG.debug("--{}: {}", opt.getLongOpt(), value); //$NON-NLS-1$
        if (value == null) {
            if (mandatory) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "option \"--{0}\" is mandatory",
                        opt.getLongOpt()));
            }
            return null;
        }
        return value;
    }

    static void process(Configuration configuration) throws IOException {
        GraphSpecView graph = load(configuration.input, configuration.repository);
        if (graph == null || graph.getVertices().isEmpty()) {
            throw new IOException(MessageFormat.format(
                    "{0} does not contain valid DAG",
                    configuration.input));
        }
        List<VertexSpecView> targets = graph.getVertices();
        if (configuration.vertexId != null) {
            targets = graph.getVertices().stream()
                    .filter(v -> v.getId().equals(configuration.vertexId))
                    .collect(Collectors.toList());
            if (targets.isEmpty()) {
                throw new IOException(MessageFormat.format(
                        "vertex \"{1}\" is not found: {0}",
                        configuration.input,
                        configuration.vertexId));
            }
        }
        try (VertexReporter reporter = configuration.reporter.get()) {
            for (VertexSpecView vertex : targets) {
                reporter.report(vertex, graph);
            }
        }
    }

    static GraphSpecView load(File file, InspectionNodeRepository repository) throws IOException {
        InspectionNode node = load0(file, repository);
        GraphSpecView graph = GraphSpecView.parse(node);
        if (graph == null) {
            throw new IOException(MessageFormat.format(
                    "{0} does not contain valid DAG",
                    file));
        }
        return graph;
    }

    private static InspectionNode load0(File file, InspectionNodeRepository repository) throws IOException {
        LOG.debug("loading file: {}", file); //$NON-NLS-1$
        if (file.getName().endsWith(".jar")) {
            LOG.debug("extracting file from jar: {} ({})", file, M3bpPackage.PATH_PLAN_INSPECTION); //$NON-NLS-1$
            try (ZipFile zip = new ZipFile(file)) {
                ZipEntry entry = zip.getEntry(M3bpPackage.PATH_PLAN_INSPECTION.toPath());
                if (entry == null) {
                    throw new FileNotFoundException(MessageFormat.format(
                            "{0} does not contain {1}",
                            file, M3bpPackage.PATH_PLAN_INSPECTION));
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    return repository.load(input);
                }
            }
        } else {
            try (InputStream input = new FileInputStream(file)) {
                return repository.load(input);
            }
        }
    }

    private static class Opts {

        final Option input = required("input", 1) //$NON-NLS-1$
                .withDescription("target jobflow library")
                .withArgumentDescription("/path/to/jobflow-somethig.jar"); //$NON-NLS-1$

        final Option vertex = optional("vertex", 1) //$NON-NLS-1$
                .withDescription("target vertex ID")
                .withArgumentDescription("vertex-id"); //$NON-NLS-1$

        final Options options = new Options();

        Opts() {
            for (Field field : Opts.class.getDeclaredFields()) {
                if (Option.class.isAssignableFrom(field.getType()) == false) {
                    continue;
                }
                try {
                    Option option = (Option) field.get(this);
                    options.addOption(option);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        private static RichOption optional(String name, int arguments) {
            return new RichOption(null, name, arguments, false);
        }

        private static RichOption required(String name, int arguments) {
            return new RichOption(null, name, arguments, true);
        }
    }

    static class Configuration {

        File input;

        String vertexId;

        InspectionNodeRepository repository = new JsonInspectionNodeRepository();

        Supplier<? extends VertexReporter> reporter = () -> new BasicVertexReporter(System.out, false);
    }
}
