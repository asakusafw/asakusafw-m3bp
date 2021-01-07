/**
 * Copyright 2011-2021 Asakusa Framework Team.
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
package com.asakusafw.m3bp.mirror.jna;

import java.io.File;

import com.asakusafw.lang.utils.common.Arguments;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;

/**
 * Loads native libraries.
 */
public class NativeFunctionLoader implements AutoCloseable {

    private final NativeLibrary library;

    /**
     * Creates a new instance.
     * @param file the target library file
     * @throws UnsatisfiedLinkError if library file is not valid
     */
    public NativeFunctionLoader(File file) {
        Arguments.requireNonNull(file);
        this.library = NativeLibrary.getInstance(file.getAbsolutePath());
    }

    /**
     * Creates a new instance.
     * @param name the target library name
     * @throws UnsatisfiedLinkError if library file is not valid
     */
    public NativeFunctionLoader(String name) {
        Arguments.requireNonNull(name);
        this.library = NativeLibrary.getInstance(name);
    }

    /**
     * Returns a value comparator.
     * @param name the target function name
     * @return the loaded comparator
     * @throws UnsatisfiedLinkError if there is no such a function
     */
    public BufferComparator getComparator(String name) {
        Arguments.requireNonNull(name);
        Function func = library.getFunction(name);
        return (a, b) -> func.invokeInt(new Object[] { a, b }) != 0;
    }

    @Override
    public void close() {
        library.dispose();
    }
}
