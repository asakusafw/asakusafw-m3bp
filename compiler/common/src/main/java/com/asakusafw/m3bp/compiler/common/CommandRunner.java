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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.lang.compiler.common.Diagnostic;
import com.asakusafw.lang.compiler.common.DiagnosticException;

/**
 * Executes operating system commands.
 */
public final class CommandRunner {

    static final Logger LOG = LoggerFactory.getLogger(CommandRunner.class);

    /**
     * Runs the command.
     * @param command the command location
     * @param workingDir the working directory
     * @param arguments the command arguments
     */
    public static void run(File command, File workingDir, String... arguments) {
        Arguments.requireNonNull(command);
        Arguments.requireNonNull(workingDir);
        Arguments.requireNonNull(arguments);
        List<String> commandLine = new ArrayList<>();
        commandLine.add(command.getAbsolutePath());
        Collections.addAll(commandLine, arguments);
        ProcessBuilder builder = new ProcessBuilder(commandLine);
        builder.directory(workingDir);
        if (workingDir.mkdirs() == false && workingDir.isDirectory() == false) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "failed to create the working directory: {0}",
                    workingDir));
        }
        LOG.debug("exec: {}", builder.command());
        try {
            Process process = builder.start();
            try {
                int status = handle(process, command.getName());
                if (status != 0) {
                    throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                            "command returns non-zero exit status: {0} (status={1})",
                            command,
                            String.valueOf(status)));
                }
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "error occurred while executing command: {0}",
                    command), e);
        } catch (InterruptedException e) {
            throw new DiagnosticException(Diagnostic.Level.ERROR, MessageFormat.format(
                    "interrupted while executing command: {0}",
                    command), e);
        }
    }

    private static int handle(Process process, String label) throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, String.format("%s-%d", label, counter.incrementAndGet())); //$NON-NLS-1$
            thread.setDaemon(true);
            return thread;
        });
        try (ReaderRedirector stdIn = new ReaderRedirector(
                process.getInputStream(),
                s -> LOG.debug("{}: {}", label, s));
                ReaderRedirector stdErr = new ReaderRedirector(
                        process.getErrorStream(),
                        s -> LOG.warn("{}: {}", label, s))) {
            Future<?> output = executor.submit(stdIn);
            Future<?> error = executor.submit(stdErr);
            output.get();
            error.get();
        } catch (IOException | ExecutionException e) {
            LOG.warn("error occurred while reading output", e);
        } finally {
            executor.shutdownNow();
        }
        return process.waitFor();
    }

    private CommandRunner() {
        return;
    }
}
