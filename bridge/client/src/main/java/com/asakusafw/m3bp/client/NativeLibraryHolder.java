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
package com.asakusafw.m3bp.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.lang.utils.common.Arguments;

/**
 * Provides native library files.
 */
public class NativeLibraryHolder implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NativeLibraryHolder.class);

    private static final NativeLibraryHolder NULL = new NativeLibraryHolder(null, false);

    private final File file;

    private final boolean deleteOnClose;

    /**
     * Creates a new instance.
     * @param file the target library file (nullable)
     * @param deleteOnClose deletes {@code true} to delete the target library on close (only if it exists),
     *     or {@code false} to keep it
     */
    public NativeLibraryHolder(File file, boolean deleteOnClose) {
        this.file = file;
        this.deleteOnClose = deleteOnClose;
    }

    /**
     * Extracts a library file on the class-path.
     * @param loader the resource loader
     * @param path the library file path
     * @return the holder
     * @throws IOException if I/O error was occurred while extracting the library
     */
    public static NativeLibraryHolder extract(ClassLoader loader, String path) throws IOException {
        Arguments.requireNonNull(loader);
        if (path == null) {
            return NULL;
        }
        LOG.debug("searching native application library: {}", path);
        File f = new File(path);
        if (f.isAbsolute() && f.isFile()) {
            LOG.debug("using external native application library: {}", f); //$NON-NLS-1$
            return new NativeLibraryHolder(f, false);
        }

        LOG.debug("extracting native application library: {}", path); //$NON-NLS-1$
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) {
                LOG.debug("missing native application library: path={}", path); //$NON-NLS-1$
                return NULL;
            }
            File target = File.createTempFile("asakusa-", System.mapLibraryName("app")); //$NON-NLS-1$ //$NON-NLS-2$
            LOG.debug("creating copy of native application library: {}", target); //$NON-NLS-1$
            Files.copy(in, target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            if (LOG.isDebugEnabled()) {
                URL url = loader.getResource(path);
                LOG.debug("using internal native application library: {}", url); //$NON-NLS-1$
            }
            return new NativeLibraryHolder(target, true);
        }
    }

    /**
     * Returns the library file.
     * @return the file, or {@code null} if this does not hold anything
     */
    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        if (deleteOnClose && file.exists()) {
            LOG.debug("deleting library file: {}", file); //$NON-NLS-1$
            if (file.delete() == false) {
                LOG.warn(MessageFormat.format(
                        "failed to delete library file: {0}",
                        file));
            }
        }
    }
}
