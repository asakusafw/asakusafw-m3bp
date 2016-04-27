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
package com.asakusafw.m3bp.compiler.core.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.compiler.codegen.NativeValueComparatorExtension;
import com.asakusafw.dag.compiler.codegen.NativeValueComparatorGenerator;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.dag.utils.common.Io;
import com.asakusafw.dag.utils.common.Lang;
import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.dag.utils.common.Tuple;
import com.asakusafw.lang.compiler.api.CompilerOptions;
import com.asakusafw.lang.compiler.api.DataModelLoader;
import com.asakusafw.lang.compiler.api.reference.DataModelReference;
import com.asakusafw.lang.compiler.common.Diagnostic;
import com.asakusafw.lang.compiler.common.DiagnosticException;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.core.JobflowCompiler.Context;
import com.asakusafw.lang.compiler.core.adapter.DataModelLoaderAdapter;
import com.asakusafw.lang.compiler.core.basic.AbstractCompilerParticipant;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.description.ReifiableTypeDescription;
import com.asakusafw.lang.compiler.model.description.TypeDescription;
import com.asakusafw.lang.compiler.model.description.TypeDescription.TypeKind;
import com.asakusafw.lang.compiler.model.graph.Group;
import com.asakusafw.lang.compiler.model.graph.Group.Ordering;
import com.asakusafw.lang.compiler.model.graph.Jobflow;
import com.asakusafw.lang.compiler.model.info.BatchInfo;
import com.asakusafw.lang.compiler.packaging.FileContainer;
import com.asakusafw.lang.compiler.packaging.FileContainerRepository;
import com.asakusafw.lang.compiler.packaging.ResourceRepository;

/**
 * A compiler participant for enabling {@link NativeValueComparatorExtension}.
 *
 * <h3> compiler options </h3>
 * <ul>
 * <li> {@code m3bp.native.path} (optional)
 *   <ul>
 *   <li> the custom command search path (separated by command separator) </li>
 *   <li> <em>default value</em>: {@code $PATH} </li>
 *   </ul>
 * </li>
 * <li> {@code m3bp.native.cmake} (optional)
 *   <ul>
 *   <li> the custom CMAKE command name or path </li>
 *   <li> <em>default value</em>: {@code cmake} </li>
 *   </ul>
 * </li>
 * <li> {@code m3bp.native.make} (optional)
 *   <ul>
 *   <li> the custom MAKE command name or path </li>
 *   <li> <em>default value</em>: {@code make} </li>
 *   </ul>
 * </li>
 * </ul>
 */
public class NativeValueComparatorParticipant extends AbstractCompilerParticipant {

    static final Logger LOG = LoggerFactory.getLogger(NativeValueComparatorParticipant.class);

    static final String KEY_PREFIX = "m3bp.native.";

    /**
     * The compiler option key of custom {@code cmake} command name or path.
     */
    public static final String KEY_CMAKE = KEY_PREFIX + "cmake";

    /**
     * The compiler option key of custom {@code make} command name or path.
     */
    public static final String KEY_MAKE = KEY_PREFIX + "make";

    /**
     * The compiler option key of custom command search path.
     */
    public static final String KEY_PATH = KEY_PREFIX + "path";

    /**
     * The compiler option key prefix of custom CMake options.
     */
    public static final String KEY_CMAKE_OPTION_PREFIX = KEY_PREFIX + "cmake.";

    /**
     * The default CMake command name.
     */
    public static final String DEFAULT_CMAKE = "cmake";

    /**
     * The default Make command name.
     */
    public static final String DEFAULT_MAKE = "make";

    private static final String OPT_CMAKE_BUILD_TYPE = "CMAKE_BUILD_TYPE";

    /**
     * The default CMake build type.
     */
    public static final String DEFAULT_CMAKE_BUILD_TYPE = "Release";

    static final Charset ENCODING = StandardCharsets.UTF_8;

    static final Location PATH_CMAKE_LISTS = Location.of("CMakeLists.txt");

    static final Location PATH_HEADER_DIR = Location.of("include"); //$NON-NLS-1$

    static final Location PATH_SOURCE_DIR = Location.of("src"); //$NON-NLS-1$

    static final Location PATH_BUILD_DIR = Location.of("build");

    static final Location PATH_SOURCE = PATH_SOURCE_DIR.append("application.cpp");

    static final Location PATH_OUTPUT_BASE = Location.of("com/asakusafw/m3bp/generated/native");

    static final String ARTIFACCT_LIB_NAME = "application";

    private static final Set<String> ARTIFACT_NAME = Lang.let(new HashSet<>(), s -> {
        s.add("libapplication.so");
        s.add("libapplication.dylib");
        s.add("application.dll");
    });

    private static final String[] FILE_CMAKE_LISTS = {
            "cmake_minimum_required(VERSION 2.8)",
            "project(all)",
            "set(CMAKE_SKIP_RPATH ON)",
            "file(GLOB NATIVE \"src/*.cpp\")",
            "include_directories(\"include\")",
            "add_library(application SHARED ${NATIVE})",
            "set_target_properties(application PROPERTIES INTERPROCEDURAL_OPTIMIZATION ON)",
            "set_target_properties(application PROPERTIES COMPILE_FLAGS \"-std=c++11 -Wall\")",
    };

    @Override
    public void beforeJobflow(Context context, BatchInfo batch, Jobflow jobflow) {
        LOG.debug("enabling {}", NativeValueComparatorExtension.class.getName());
        context.registerExtension(NativeValueComparatorExtension.class, createExtension(
                context.getOptions(),
                createTemporaryOutput(context.getTemporaryOutputs(), jobflow.getFlowId()),
                new DataModelLoaderAdapter(context)));
    }

    @Override
    public void afterJobflow(Context context, BatchInfo batch, Jobflow jobflow) {
        NativeValueComparatorExtension extension = context.getExtension(NativeValueComparatorExtension.class);
        if (processExtension(extension, context.getOutput())) {
            context.registerExtension(NativeValueComparatorExtension.class, null);
        }
    }

    static NativeValueComparatorExtension createExtension(
            CompilerOptions options, FileContainer working, DataModelLoader loader) {
        CommandPath path = getCommandPath(options);
        File cmake = getCmakeCommand(options, path);
        LOG.debug("cmake: {}", cmake);

        File make = getMakeCommand(options, path);
        LOG.debug("make: {}", make);

        Map<String, String> cmakeOptions = getCmakeOptions(options);
        LOG.debug("cmake options: {}", cmake);

        Support extension = new Support(loader, working, cmake, make, cmakeOptions);
        return extension;
    }

    private static Map<String, String> getCmakeOptions(CompilerOptions options) {
        Map<String, String> results = new LinkedHashMap<>();
        options.getProperties(KEY_CMAKE_OPTION_PREFIX).forEach((k, v) -> {
            results.put(k.substring(KEY_CMAKE_OPTION_PREFIX.length()), v);
        });
        results.putIfAbsent(OPT_CMAKE_BUILD_TYPE, DEFAULT_CMAKE_BUILD_TYPE);
        return results;
    }

    static boolean processExtension(NativeValueComparatorExtension extension, FileContainer output) {
        if (extension instanceof Support) {
            Conf conf = null;
            try (Support s = (Support) extension) {
                if (s.added) {
                    conf = s.conf;
                }
            }
            if (conf != null) {
                process(conf.base, conf.cmake, conf.make, conf.cmakeOptions, output);
            }
            return true;
        }
        return false;
    }

    static void process(
            FileContainer project,
            File cmake, File make,
            Map<String, String> cmakeOptions,
            FileContainer output) {
        genCmakeLists(project);

        List<String> cmakeArgs = new ArrayList<>();
        cmakeArgs.add(project.getBasePath().getAbsolutePath());
        cmakeArgs.add("-G");
        cmakeArgs.add("Unix Makefiles");
        cmakeOptions.forEach((k, v) -> cmakeArgs.add(String.format("-D%s=%s", k, v)));
        CommandRunner.run(cmake, project.toFile(PATH_BUILD_DIR), cmakeArgs.stream().toArray(String[]::new));

        CommandRunner.run(make, project.toFile(PATH_BUILD_DIR));

        copyArtifact(project, output);
    }

    static void copyArtifact(FileContainer project, FileContainer output) {
        try (ResourceRepository.Cursor cursor = project.createCursor()) {
            while (cursor.next()) {
                Location location = cursor.getLocation();
                if (PATH_BUILD_DIR.isPrefixOf(location) == false) {
                    continue;
                }
                if (ARTIFACT_NAME.contains(location.getName())) {
                    Location destination = PATH_OUTPUT_BASE.append(location.getName());
                    LOG.debug("copy native lib: {} -> {}",
                            location,
                            destination);
                    try (InputStream contents = cursor.openResource()) {
                        output.addResource(destination, contents);
                    }
                    return;
                }
            }
        } catch (IOException e) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "error occurred while copying generated native library: {0}",
                    project.toFile(PATH_BUILD_DIR)), e);
        }
        throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                "cannot detect the generated native library: {0}",
                project.toFile(PATH_BUILD_DIR)));
    }

    static void genCmakeLists(FileContainer project) {
        try (OutputStream output = project.addResource(PATH_CMAKE_LISTS);
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, ENCODING))) {
            for (String line : FILE_CMAKE_LISTS) {
                writer.println(line);
            }
        } catch (IOException e) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "error occurred while generating file: {0}",
                    project.toFile(PATH_CMAKE_LISTS)), e);
        }
    }

    static CommandPath getCommandPath(CompilerOptions options) {
        return Optionals.of(options.get(KEY_PATH, System.getenv("PATH")))
                .map(CommandPath::of)
                .orElseGet(() -> new CommandPath(Collections.emptyList()));
    }

    static File getCmakeCommand(CompilerOptions options, CommandPath path) {
        return getCommand(path, options.get(KEY_CMAKE, DEFAULT_CMAKE));
    }

    static File getMakeCommand(CompilerOptions options, CommandPath path) {
        return getCommand(path, options.get(KEY_MAKE, DEFAULT_MAKE));
    }

    static File getCommand(CommandPath path, String command) {
        return Optionals.of(path.find(command))
                .map(File::getAbsoluteFile)
                .orElseThrow(() -> new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                        "failed to detect command: {0}",
                        command)));
    }

    static FileContainer createTemporaryOutput(FileContainerRepository outputs, String flowId) {
        String prefix = String.format("m3bp-%s", flowId); //$NON-NLS-1$
        try {
            return outputs.newContainer(prefix);
        } catch (IOException e) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "failed to create temporary output: {0}",
                    outputs.getRoot()));
        }
    }

    private static class Conf {

        final DataModelLoader loader;

        final FileContainer base;

        final File cmake;

        final File make;

        final Map<String, String> cmakeOptions;

        Conf(DataModelLoader loader, FileContainer base,
                File cmake, File make,
                Map<String, String> cmakeOptions) {
            this.loader = loader;
            this.base = base;
            this.cmake = cmake;
            this.make = make;
            this.cmakeOptions = Arguments.freeze(cmakeOptions);
        }
    }

    private static class Support implements NativeValueComparatorExtension, Io {

        final Conf conf;

        NativeValueComparatorGenerator generator;

        boolean added = false;

        Support(DataModelLoader loader, FileContainer base,
                File cmake, File make,
                Map<String, String> cmakeOptions) {
            this.conf = new Conf(loader, base, cmake, make, cmakeOptions);
        }

        private static final String FORMAT_FUNC = "lt_%s_%d";

        private final Map<Tuple<ClassDescription, List<Group.Ordering>>, String> cache = new HashMap<>();

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public String addComparator(TypeDescription type, Group grouping) {
            List<Ordering> ordering = grouping.getOrdering();
            if (ordering.isEmpty()) {
                return null;
            }
            ReifiableTypeDescription erasure = type.getErasure();
            Invariants.require(erasure.getTypeKind() == TypeKind.CLASS);
            ClassDescription dataType = (ClassDescription) erasure;

            Tuple<ClassDescription, List<Group.Ordering>> key = new Tuple<>(dataType, ordering);
            synchronized (this) {
                String cached = cache.get(key);
                if (cached != null) {
                    return cached;
                }
                String name = String.format(FORMAT_FUNC, dataType.getSimpleName(), counter.incrementAndGet());
                defineComparator(dataType, ordering, name);
                cache.put(key, name);
                return name;
            }
        }

        private void defineComparator(ClassDescription type, List<Group.Ordering> orderings, String name) {
            if (generator == null) {
                NativeValueComparatorGenerator.copyHeaderFiles(conf.base, PATH_HEADER_DIR);
                NativeValueComparatorGenerator.copySourceFiles(conf.base, PATH_SOURCE_DIR);
                generator = new NativeValueComparatorGenerator(open());
                added = true;
            }
            DataModelReference reference = conf.loader.load(type);
            generator.add(reference, orderings, name);
        }

        private PrintWriter open() {
            try {
                return new PrintWriter(new OutputStreamWriter(conf.base.addResource(PATH_SOURCE), ENCODING));
            } catch (IOException e) {
                throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                        "failed to create a file: {0}",
                        conf.base.toFile(PATH_SOURCE)));
            }
        }

        @Override
        public void close() {
            if (generator != null) {
                generator.close();
            }
        }
    }
}
