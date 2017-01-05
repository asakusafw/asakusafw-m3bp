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
package com.asakusafw.m3bp.mirror.jni;

import java.text.MessageFormat;

/**
 * An abstract super interface of native mirror objects.
 */
public interface NativeMirror {

    /**
     * Returns the native object reference.
     * @return the native object reference
     */
    Pointer getPointer();

    /**
     * Returns the native object reference of the target object.
     * @param object the target object
     * @return the corresponded native reference
     * @throws IllegalStateException the target object is not a native mirror
     */
    static Pointer getPointer(Object object) {
        if ((object instanceof NativeMirror) == false) {
            throw new IllegalStateException(MessageFormat.format(
                    "object must be a native mirror: {0}", //$NON-NLS-1$
                    object));
        }
        return ((NativeMirror) object).getPointer();
    }
}
