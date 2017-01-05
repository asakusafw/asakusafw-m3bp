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
package com.asakusafw.m3bp.mirror;

import java.io.File;

/**
 * A mirror of M3BP configuration.
 * @since 0.1.0
 * @version 0.1.1
 */
public interface ConfigurationMirror {

    /**
     * Returns the max number of concurrent executions.
     * @return the default max concurrency
     */
    int getMaxConcurrency();

    /**
     * Sets the max number of concurrent executions.
     * @param newValue the default max concurrency
     * @return this
     */
    ConfigurationMirror withMaxConcurrency(int newValue);

    /**
     * Returns the number of partitions in scatter-gather operation.
     * @return the default partition count
     */
    int getPartitionCount();

    /**
     * Sets the number of partitions in scatter-gather operation.
     * @param newValue the default partition count
     * @return this
     */
    ConfigurationMirror withPartitionCount(int newValue);

    /**
     * Returns the output buffer size in bytes.
     * @return the output buffer size
     */
    long getOutputBufferSize();

    /**
     * Sets the output buffer size in bytes.
     * @param newValue the output buffer size
     * @return this
     */
    ConfigurationMirror withOutputBufferSize(long newValue);

    /**
     * Returns the flush factor of output buffer.
     * @return the flush factor
     * @since 0.1.1
     */
    float getOutputBufferFlushFactor();

    /**
     * Sets the flush factor of output buffer.
     * @param newValue the flush factor
     * @return this
     * @since 0.1.1
     */
    ConfigurationMirror withOutputBufferFlushFactor(float newValue);

    /**
     * Returns the number of available records in each output buffer.
     * @return the maximum number of records per buffer
     */
    long getOutputRecordsPerBuffer();

    /**
     * Sets the number of available records in each output buffer.
     * @param newValue the maximum number of records per buffer
     * @return this
     */
    ConfigurationMirror withOutputRecordsPerBuffer(long newValue);

    /**
     * Returns the affinity mode.
     * @return the affinity mode
     */
    AffinityMode getAffinityMode();

    /**
     * Sets the affinity mode of worker threads.
     * @param newValue the affinity mode
     * @return this
     */
    ConfigurationMirror withAffinityMode(AffinityMode newValue);

    /**
     * Returns the buffer access mode.
     * @return the buffer access mode
     */
    BufferAccessMode getBufferAccessMode();

    /**
     * Sets the buffer access mode.
     * @param newValue the buffer access mode
     * @return this
     */
    ConfigurationMirror withBufferAccessMode(BufferAccessMode newValue);

    /**
     * Returns the profiling information output file.
     * @return the profiling information output file
     */
    File getProfilingOutput();

    /**
     * Sets the profiling information destination.
     * @param newValue the target file (nullable)
     * @return this
     */
    ConfigurationMirror withProfilingOutput(File newValue);

    /**
     * Represents kinds of buffer access mode.
     */
    enum BufferAccessMode {

        /**
         * Uses Java NIO to access to the edge buffers.
         */
        NIO,

        /**
         * Uses Unsafe to access to the edge buffers.
         */
        UNSAFE,
    }

    /**
     * Represents kinds of thread affinity mode.
     */
    enum AffinityMode {

        /**
         * Nothing special.
         */
        NONE(0),

        /**
         * Compact mode.
         */
        COMPACT(1),

        /**
         * Scatter mode.
         */
        SCATTER(2),
        ;

        private final int id;

        AffinityMode(int id) {
            this.id = id;
        }

        /**
         * Returns the enum ID.
         * @return the enum ID
         */
        public int getId() {
            return id;
        }

        /**
         * Restores the enum constant from its ID.
         * @param id the ID
         * @return the corresponded enum constant
         * @see #getId()
         */
        public static AffinityMode fromId(int id) {
            return values()[id];
        }
    }
}
