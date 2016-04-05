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

import com.asakusafw.lang.compiler.common.Location;

/**
 * Constants about Asakusa on M3BP jobflow packages.
 */
public final class M3bpPackage {

    /**
     * The common generated class prefix.
     */
    public static final String CLASS_PREFIX = "com.asakusafw.m3bp.generated."; //$NON-NLS-1$

    /**
     * The application class name.
     */
    public static final String CLASS_APPLICATION = CLASS_PREFIX + "Application"; //$NON-NLS-1$

    /**
     * The DAG information location.
     */
    public static final Location PATH_GRAPH_INFO = Location.of("META-INF/asakusa-m3bp/dag.bin"); //$NON-NLS-1$

    /**
     * The plan inspection information location.
     */
    public static final Location PATH_PLAN_INSPECTION = Location.of("META-INF/asakusa-m3bp/plan.json"); //$NON-NLS-1$

    private M3bpPackage() {
        return;
    }
}
