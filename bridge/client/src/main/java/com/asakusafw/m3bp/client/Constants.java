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
package com.asakusafw.m3bp.client;

/**
 * Constants for M3BP client.
 * @since 0.1.0
 * @version 0.1.1
 */
public final class Constants {

    /**
     * The common generated class prefix.
     */
    public static final String CLASS_PREFIX = "com.asakusafw.m3bp.generated."; //$NON-NLS-1$

    /**
     * The application native library directory (in class-path).
     */
    public static final String NATIVE_LIBRARY_DIR = CLASS_PREFIX.replace('.', '/') + "native"; //$NON-NLS-1$

    /**
     * The application native library name.
     */
    public static final String NATIVE_LIBRARY_NAME = "application"; //$NON-NLS-1$

    /**
     * The application native library path.
     */
    public static final String NATIVE_LIBRARY_PATH = String.format(
            "%s/%s", //$NON-NLS-1$
            NATIVE_LIBRARY_DIR, System.mapLibraryName(NATIVE_LIBRARY_NAME));

    /**
     * The M3BP engine configuration key prefix.
     */
    public static final String KEY_ENGINE_PREFIX = "com.asakusafw.m3bp."; //$NON-NLS-1$

    /**
     * The configuration key of max worker threads.
     */
    public static final String KEY_THREAD_MAX = KEY_ENGINE_PREFIX + "thread.max"; //$NON-NLS-1$

    /**
     * The configuration key of thread affinity mode.
     */
    public static final String KEY_THREAD_AFFINITY = KEY_ENGINE_PREFIX + "thread.affinity"; //$NON-NLS-1$

    /**
     * The configuration key of default number of partitions.
     */
    public static final String KEY_PARTITIONS = KEY_ENGINE_PREFIX + "partitions"; //$NON-NLS-1$

    /**
     * The configuration key of output buffer size.
     */
    public static final String KEY_OUTPUT_BUFFER_SIZE = KEY_ENGINE_PREFIX + "output.buffer.size"; //$NON-NLS-1$

    /**
     * The configuration key of output buffer flush factor.
     * @since 0.1.1
     */
    public static final String KEY_OUTPUT_BUFFER_FLUSH = KEY_ENGINE_PREFIX + "output.buffer.flush"; //$NON-NLS-1$

    /**
     * The configuration key of record size in each output buffer.
     */
    public static final String KEY_OUTPUT_BUFFER_RECORDS = KEY_ENGINE_PREFIX + "output.buffer.records"; //$NON-NLS-1$

    /**
     * The configuration key of buffer access mode.
     */
    public static final String KEY_BUFFER_ACCESS = KEY_ENGINE_PREFIX + "buffer.access"; //$NON-NLS-1$

    /**
     * The configuration key of core engine profiling output file.
     */
    public static final String KEY_PROFILE_OUTPUT = KEY_ENGINE_PREFIX + "profile.output"; //$NON-NLS-1$

    /**
     * The configuration key of custom native application library URI.
     */
    public static final String KEY_NATIVE_LIBRARY = KEY_ENGINE_PREFIX + "application.library"; //$NON-NLS-1$

    /**
     * The configuration key of whether mock engine implementation is allowed or not.
     * @see Capability
     */
    public static final String KEY_ENGINE_MOCK = KEY_ENGINE_PREFIX + "engine.mock"; //$NON-NLS-1$

    /**
     * The configuration key prefix of Hadoop settings.
     */
    public static final String KEY_HADOOP_PREFIX = "hadoop.";

    private Constants() {
        return;
    }
}
