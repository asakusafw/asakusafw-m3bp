/*
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
package com.asakusafw.m3bp.gradle.plugins

/**
 * An extension object for {@code asakusafwOrgnizer.m3bp}.
 */
class AsakusafwOrganizerM3bpExtension {

    /**
     * Configuration whether {@code 'Asakusa on M3'} features are enabled or not.
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
     * Configuration whether {@code 'Asakusa on M3'} uses the system native dependency libraries.
     * <dl>
     *   <dt> Default value: </dt>
     *     <dd> {@code false} </dd>
     * </dl>
     */
    boolean useSystemNativeDependencies

    /**
     * Configuration whether {@code 'Asakusa on M3'} uses the system Hadoop installation.
     * If this is {@code true}, Asakusa on M3 will require a Hadoop installation for the environment.
     * Otherwise, the minimal Hadoop libraries will be bundled into the deployment archives.
     * <dl>
     *   <dt> Default value: </dt>
     *     <dd> {@code false} </dd>
     * </dl>
     */
    boolean useSystemHadoop
}
