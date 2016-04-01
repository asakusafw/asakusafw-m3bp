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
package com.asakusafw.m3bp.compiler.core;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.function.Function;

import com.asakusafw.dag.compiler.model.ClassData;
import com.asakusafw.dag.utils.common.Action;
import com.asakusafw.lang.compiler.api.JobflowProcessor;
import com.asakusafw.lang.compiler.api.JobflowProcessor.Context;
import com.asakusafw.lang.compiler.common.Diagnostic;
import com.asakusafw.lang.compiler.common.DiagnosticException;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.info.JobflowInfo;

/**
 * Represents a compiler context.
 */
public interface M3bpCompilerContext {

    /**
     * Returns the root context.
     * @return the root context
     */
    JobflowProcessor.Context getRoot();

    /**
     * Returns the target jobflow info.
     * @return the jobflow info
     */
    JobflowInfo getInfo();

    /**
     * Returns the class name provider.
     * @return the class name provider
     */
    Function<String, ClassDescription> getClassNameProvider();

    /**
     * Generates a new class and adds it as a compilation result.
     * @param category the generating class category
     * @param generator the class generator
     * @return the generated class description
     */
    default ClassDescription generate(String category, Function<ClassDescription, ClassData> generator) {
        ClassDescription target = getClassNameProvider().apply(category);
        ClassData generated = generator.apply(target);
        return add(generated);
    }

    /**
     * Adds a resource file as a compilation result.
     * @param location the target location
     * @param action the resource adding action
     */
    default void add(Location location, Action<OutputStream, IOException> action) {
        try (OutputStream output = getRoot().addResourceFile(location)) {
            action.perform(output);
        } catch (IOException e) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "error occurred while adding a resource: {0}",
                    location), e);
        }
    }

    /**
     * Adds class data as a compilation result.
     * @param generated the generated class data
     * @return the target class description
     */
    default ClassDescription add(ClassData generated) {
        if (generated.isProvided() == false) {
            try (OutputStream output = getRoot().addClassFile(generated.getDescription())) {
                generated.dump(output);
            } catch (IOException e) {
                throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                        "error occurred while generating a class file: {0}",
                        generated.getDescription().getBinaryName()), e);
            }
        }
        return generated.getDescription();
    }

    /**
     * A basic implementation of {@link M3bpCompilerContext}.
     */
    class Basic implements M3bpCompilerContext {

        private final JobflowProcessor.Context root;

        private final JobflowInfo info;

        private final ClassNameProvider namer = new ClassNameProvider();

        /**
         * Creates a new instance.
         * @param root the root context
         * @param info the target jobflow info
         */
        public Basic(Context root, JobflowInfo info) {
            this.root = root;
            this.info = info;
        }

        @Override
        public Context getRoot() {
            return root;
        }

        @Override
        public JobflowInfo getInfo() {
            return info;
        }

        @Override
        public Function<String, ClassDescription> getClassNameProvider() {
            return namer;
        }
    }
}
