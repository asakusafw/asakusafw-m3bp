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
package com.asakusafw.m3bp.mirror.basic;

import java.io.File;

import com.asakusafw.m3bp.mirror.ConfigurationMirror;

/**
 * A basic implementation of {@link ConfigurationMirror}.
 * @since 0.1.0
 * @version 0.1.1
 */
public class BasicConfigurationMirror implements ConfigurationMirror {

    private int maxConcurrency = 1;

    private int partitionCount = 1;

    private long outputBufferSize = 256 * 1024;

    private float outputBufferFlushFactor = .8f;

    private long recordsPerBuffer = 16 * 1024;

    private BufferAccessMode bufferAccessMode = BufferAccessMode.NIO;

    private AffinityMode affinityMode = AffinityMode.NONE;

    private File profilingOutput;

    @Override
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    @Override
    public ConfigurationMirror withMaxConcurrency(int newValue) {
        maxConcurrency = newValue;
        return this;
    }

    @Override
    public int getPartitionCount() {
        return partitionCount;
    }

    @Override
    public ConfigurationMirror withPartitionCount(int newValue) {
        partitionCount = newValue;
        return this;
    }

    @Override
    public long getOutputBufferSize() {
        return outputBufferSize;
    }

    @Override
    public ConfigurationMirror withOutputBufferSize(long newValue) {
        outputBufferSize = newValue;
        return this;
    }

    @Override
    public float getOutputBufferFlushFactor() {
        return outputBufferFlushFactor;
    }

    @Override
    public ConfigurationMirror withOutputBufferFlushFactor(float newValue) {
        outputBufferFlushFactor = Math.min(.9f, Math.max(.1f, newValue));
        return this;
    }

    @Override
    public long getOutputRecordsPerBuffer() {
        return recordsPerBuffer;
    }

    @Override
    public ConfigurationMirror withOutputRecordsPerBuffer(long newValue) {
        recordsPerBuffer = newValue;
        return this;
    }

    @Override
    public AffinityMode getAffinityMode() {
        return affinityMode;
    }

    @Override
    public ConfigurationMirror withAffinityMode(AffinityMode newValue) {
        affinityMode = newValue;
        return this;
    }

    @Override
    public BufferAccessMode getBufferAccessMode() {
        return bufferAccessMode;
    }

    @Override
    public ConfigurationMirror withBufferAccessMode(BufferAccessMode newValue) {
        this.bufferAccessMode = newValue;
        return this;
    }

    @Override
    public File getProfilingOutput() {
        return profilingOutput;
    }

    @Override
    public ConfigurationMirror withProfilingOutput(File newValue) {
        profilingOutput = newValue;
        return this;
    }
}
