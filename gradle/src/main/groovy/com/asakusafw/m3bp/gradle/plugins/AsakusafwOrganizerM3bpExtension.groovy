/*
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
package com.asakusafw.m3bp.gradle.plugins

import org.gradle.api.Project

/**
 * An extension object for {@code asakusafwOrgnizer.m3bp}.
 * @since 0.1.0
 * @version 0.3.0
 */
class AsakusafwOrganizerM3bpExtension {

    /**
     * Configuration whether {@code 'Asakusa on M3BP'} features are enabled or not.
     * M3BP facilities will be enabled only if this value is {@code true}.
     * <dl>
     *   <dt> Default value: </dt>
     *     <dd> {@code true} </dd>
     * </dl>
     */
    boolean enabled

    /**
     * Configuration whether M3BP native feature is enabled or not.
     * <dl>
     *   <dt> Default value: </dt>
     *     <dd> {@code true} </dd>
     * </dl>
     */
    boolean nativeEnabled

    /**
     * Configuration whether {@code 'Asakusa on M3BP'} uses the system native dependency libraries.
     * <dl>
     *   <dt> Default value: </dt>
     *     <dd> {@code false} </dd>
     * </dl>
     */
    boolean useSystemNativeDependencies

    /**
     * NOP since {@code 0.3.0}.
     * @deprecated Use {@code asakusafwOrganizer.hadoop.embed} instead.
     */
    @Deprecated
    Object useSystemHadoop

    private Project project

    AsakusafwOrganizerM3bpExtension(Project project) {
        this.project = project
    }

    /**
     * NOP.
     * @param v ignored
     * @since 0.3.0
     * @deprecated Use {@code asakusafwOrganizer.hadoop.embed} instead.
     */
    @Deprecated
    void setUseSystemHadoop(Object v) {
        project.logger.warn "DEPRECATED: 'm3bp.useSystemHadoop' is ignored."
    }

    /**
     * NOP.
     * @since 0.3.0
     * @deprecated Use {@code asakusafwOrganizer.hadoop.embed} instead.
     */
    @Deprecated
    Object getUseSystemHadoop() {
        return null
    }
}
