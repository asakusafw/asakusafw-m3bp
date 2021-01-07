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
package com.asakusafw.m3bp.mirror.unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Utilities for accessing Unsafe API.
 */
public final class UnsafeUtil {

    private static final String CLASS_UNSAFE = "sun.misc.Unsafe";

    private static final String FIELD_UNSAFE = "theUnsafe";

    static final Object API;

    static {
        Object api = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try {
                Field f = Class.forName(CLASS_UNSAFE).getDeclaredField(FIELD_UNSAFE);
                f.setAccessible(true);
                return f.get(null);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        });
        API = api;
    }

    /**
     * Returns whether the Unsafe API is available in this environment.
     * @return {@code true} the Unsafe API is available, otherwise {@code false}
     */
    public static boolean isAvailable() {
        return API != null;
    }

    private UnsafeUtil() {
        return;
    }
}
