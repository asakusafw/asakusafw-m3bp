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
package com.asakusafw.m3bp.compiler.common;

import com.asakusafw.lang.compiler.common.Location;

/**
 * Constants about Asakusa on M3BP tasks.
 */
public final class M3bpTask {

    /**
     * The M3BP components base path (relative from the Asakusa framework installation).
     */
    public static final Location PATH_M3BP = Location.of("m3bp"); //$NON-NLS-1$

    /**
     * The M3BP bootstrap command path (relative from the Asakusa framework installation).
     */
    public static final Location PATH_COMMAND = PATH_M3BP.append(Location.of("bin/execute")); //$NON-NLS-1$

    /**
     * The M3BP configuration directory path (relative from the Asakusa framework installation).
     */
    public static final Location PATH_CONFIG_DIR = PATH_M3BP.append(Location.of("conf")); //$NON-NLS-1$

    /**
     * The M3BP engine configuration file path (relative from the Asakusa framework installation).
     */
    public static final Location PATH_ENGINE_CONFIG = PATH_CONFIG_DIR
            .append(Location.of("m3bp.properties")); //$NON-NLS-1$

    /**
     * The module name.
     */
    public static final String MODULE_NAME = "m3bp"; //$NON-NLS-1$

    /**
     * The target profile name.
     */
    public static final String PROFILE_NAME = "m3bp"; //$NON-NLS-1$

    private M3bpTask() {
        return;
    }
}
