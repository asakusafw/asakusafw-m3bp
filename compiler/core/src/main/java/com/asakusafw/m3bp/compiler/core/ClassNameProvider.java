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
package com.asakusafw.m3bp.compiler.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.asakusafw.lang.compiler.model.PropertyName;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.m3bp.compiler.common.M3bpPackage;

/**
 * Provides unique class names.
 */
public class ClassNameProvider implements Function<String, ClassDescription> {

    private final Map<String, AtomicInteger> counter = new HashMap<>();

    private final String prefix;

    /**
     * Creates a new instance.
     * @param prefix the class name prefix
     */
    public ClassNameProvider(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Creates a new instance.
     */
    public ClassNameProvider() {
        this(M3bpPackage.CLASS_PREFIX);
    }

    @Override
    public ClassDescription apply(String t) {
        String normalized = normalize(t);
        int count = counter.computeIfAbsent(normalized, s -> new AtomicInteger()).incrementAndGet();
        return new ClassDescription(String.format("%s%s._%d", //$NON-NLS-1$
                prefix, normalized, count));
    }

    private String normalize(String t) {
        if (t.indexOf('.') >= 0) {
            return t;
        }
        return PropertyName.of(t).toMemberName();
    }
}
