/**
 * Copyright 2011-2018 Asakusa Framework Team.
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
package com.asakusafw.m3bp.mirror.jni;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.m3bp.mirror.ConfigurationMirror;

/**
 * JNI bridge of {@link ConfigurationMirror}.
 * @since 0.1.0
 * @version 0.1.1
 */
public class ConfigurationMirrorImpl implements ConfigurationMirror, NativeMirror {

    private final Pointer reference;

    private float outputBufferFlushFactor = .8f;

    private BufferAccessMode bufferAccessMode = BufferAccessMode.NIO;

    ConfigurationMirrorImpl(Pointer reference) {
        Arguments.requireNonNull(reference);
        this.reference = reference;
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    public int getMaxConcurrency() {
        return (int) getMaxConcurrency0(reference.getAddress());
    }

    @Override
    public ConfigurationMirror withMaxConcurrency(int newValue) {
        setMaxConcurrency0(reference.getAddress(), newValue);
        return this;
    }

    @Override
    public int getPartitionCount() {
        return (int) getPartitionCount0(reference.getAddress());
    }

    @Override
    public ConfigurationMirror withPartitionCount(int newValue) {
        setPartitionCount0(getPointer().getAddress(), newValue);
        return this;
    }

    @Override
    public long getOutputBufferSize() {
        return getOutputBufferSize0(reference.getAddress());
    }

    @Override
    public ConfigurationMirror withOutputBufferSize(long newValue) {
        setOutputBufferSize0(getPointer().getAddress(), newValue);
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
        return getOutputRecordsPerBuffer0(reference.getAddress());
    }

    @Override
    public ConfigurationMirror withOutputRecordsPerBuffer(long newValue) {
        setOutputRecordsPerBuffer0(getPointer().getAddress(), newValue);
        return this;
    }

    @Override
    public AffinityMode getAffinityMode() {
        int id = getAffinityMode0(reference.getAddress());
        return AffinityMode.fromId(id);
    }

    @Override
    public ConfigurationMirror withAffinityMode(AffinityMode newValue) {
        Arguments.requireNonNull(newValue);
        setAffinityMode0(getPointer().getAddress(), newValue.getId());
        return this;
    }

    @Override
    public BufferAccessMode getBufferAccessMode() {
        return bufferAccessMode ;
    }

    @Override
    public ConfigurationMirror withBufferAccessMode(BufferAccessMode newValue) {
        this.bufferAccessMode = newValue;
        return this;
    }

    @Override
    public File getProfilingOutput() {
        return Optionals.of(getProfilingOutput0(reference.getAddress()))
                .map(a -> new String(a, StandardCharsets.UTF_8))
                .map(File::new)
                .orElse(null);
    }

    @Override
    public ConfigurationMirror withProfilingOutput(File newValue) {
        setProfilingOutput0(getPointer().getAddress(), Optionals.of(newValue)
                .map(File::getAbsolutePath)
                .orElse(null));
        return this;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "ConfigurationMirror[{0}]", //$NON-NLS-1$
                getPointer());
    }

    private static native long getMaxConcurrency0(long self);

    private static native void setMaxConcurrency0(long self, long newValue);

    private static native long getPartitionCount0(long self);

    private static native void setPartitionCount0(long self, long newValue);

    private static native long getOutputBufferSize0(long self);

    private static native void setOutputBufferSize0(long self, long newValue);

    private static native long getOutputRecordsPerBuffer0(long self);

    private static native void setOutputRecordsPerBuffer0(long self, long newValue);

    private static native int getAffinityMode0(long self);

    private static native void setAffinityMode0(long self, int newValue);

    private static native byte[] getProfilingOutput0(long self);

    private static native void setProfilingOutput0(long self, String newValue);
}
