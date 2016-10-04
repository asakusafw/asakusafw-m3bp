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
package com.asakusafw.m3bp.compiler.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Lang;

/**
 * Represents a command path.
 */
public class CommandPath {

    static final List<String> EXECUTABLE_EXTENSIONS;
    static {
        List<String> extensions = new ArrayList<>();
        extensions.add(""); //$NON-NLS-1$
        if (System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("windows")) {
            extensions.add(".exe");
            extensions.add(".bat");
        }
        EXECUTABLE_EXTENSIONS = extensions;
    }

    private final List<File> directories;

    /**
     * Creates a new instance.
     * @param directories the path directories
     */
    public CommandPath(List<File> directories) {
        Arguments.requireNonNull(directories);
        this.directories = Arguments.freeze(directories);
    }

    /**
     * Creates a new instance.
     * @param directories the path directories
     */
    public CommandPath(File... directories) {
        Arguments.requireNonNull(directories);
        this.directories = Arguments.freezeToList(directories);
    }

    /**
     * Returns an appended path.
     * @param other to be appended in the tail of this path
     * @return the appended path
     */
    public CommandPath append(CommandPath other) {
        Arguments.requireNonNull(other);
        return new CommandPath(Lang.concat(directories, other.directories));
    }

    /**
     * Creates a new instance.
     * @return the created instance
     */
    public static CommandPath system() {
        String pathString = System.getenv("PATH"); //$NON-NLS-1$
        return of(pathString);
    }

    /**
     * Creates a new instance.
     * @param pathString the path string
     * @return the created instance
     */
    public static CommandPath of(String pathString) {
        List<File> directories = Stream.of(pathString.split(Pattern.quote(File.pathSeparator)))
            .map(String::trim)
            .filter(s -> s.isEmpty() == false)
            .map(File::new)
            .collect(Collectors.toList());
        return new CommandPath(directories);
    }

    /**
     * Returns a resolved file path of the specified command.
     * @param command the target command name
     * @return the command file path, or {@code null} if there is no such a command
     */
    public File find(String command) {
        return Stream.concat(
                    Stream.of(new File(command)).filter(File::isAbsolute),
                    directories.stream().map(d -> new File(d, command)))
                .map(File::getPath)
                .flatMap(path -> EXECUTABLE_EXTENSIONS.stream()
                        .map(path::concat)
                        .map(File::new)
                        .filter(File::isFile)
                        .filter(File::canExecute))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the path string.
     * @return the path string
     */
    public String asPathString() {
        return directories.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
    }

    @Override
    public String toString() {
        return directories.toString();
    }
}
