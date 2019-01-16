/**
 * Copyright 2011-2019 Asakusa Framework Team.
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

import java.text.MessageFormat;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.m3bp.mirror.ConfigurationMirror;
import com.asakusafw.m3bp.mirror.ConfigurationMirror.BufferAccessMode;
import com.asakusafw.m3bp.mirror.Identifier;
import com.asakusafw.m3bp.mirror.InputReaderMirror;
import com.asakusafw.m3bp.mirror.OutputWriterMirror;
import com.asakusafw.m3bp.mirror.TaskMirror;

/**
 * JNI bridge of {@link TaskMirror}.
 * @since 0.1.0
 * @version 0.1.2
 */
public class TaskMirrorImpl implements TaskMirror, NativeMirror {

    private final Pointer reference;

    private final ConfigurationMirror configuration;

    private final boolean unsafe;

    TaskMirrorImpl(Pointer reference, ConfigurationMirror configuration) {
        Arguments.requireNonNull(reference);
        this.reference = reference;
        this.configuration = configuration;
        this.unsafe = configuration.getBufferAccessMode() == BufferAccessMode.UNSAFE;
    }

    @Override
    public Pointer getPointer() {
        return reference;
    }

    @Override
    public Identifier logicalTaskId() {
        return new Identifier(logicalTaskId0(getPointer().getAddress()));
    }

    @Override
    public Identifier phisicalTaskId() {
        return new Identifier(physicalTaskId0(getPointer().getAddress()));
    }

    @Override
    public InputReaderMirror input(Identifier id) {
        Arguments.requireNonNull(id);
        Pointer ref = new Pointer(input0(getPointer().getAddress(), id.getValue()));
        if (unsafe) {
            return new InputReaderMirrorUnsafe(ref);
        } else {
            return new InputReaderMirrorImpl(ref);
        }
    }

    @Override
    public OutputWriterMirror output(Identifier id) {
        ConfigurationMirror conf = configuration;
        Arguments.requireNonNull(id);
        Pointer ref = new Pointer(output0(getPointer().getAddress(), id.getValue()));
        if (unsafe) {
            return new OutputWriterMirrorUnsafe(ref, conf.getOutputBufferFlushFactor());
        } else {
            return new OutputWriterMirrorImpl(ref, conf.getOutputBufferFlushFactor());
        }
    }

    @Override
    public boolean isCancelled() {
        return isCancelled0(getPointer().getAddress());
    }

    @Override
    public String toString() {
        return MessageFormat.format(
                "TaskMirror[{0}]", //$NON-NLS-1$
                getPointer());
    }

    private static native long input0(long self, long id);

    private static native long output0(long self, long id);

    private static native long logicalTaskId0(long self);

    private static native long physicalTaskId0(long self);

    private static native boolean isCancelled0(long self);
}
