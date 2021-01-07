/*
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
package com.asakusafw.m3bp.gradle.plugins.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails

import com.asakusafw.gradle.plugins.AsakusafwBaseExtension
import com.asakusafw.gradle.plugins.AsakusafwBasePlugin
import com.asakusafw.gradle.plugins.AsakusafwOrganizerProfile
import com.asakusafw.gradle.plugins.internal.AbstractOrganizer
import com.asakusafw.gradle.plugins.internal.PluginUtils
import com.asakusafw.m3bp.gradle.plugins.AsakusafwOrganizerM3bpExtension

/**
 * Processes an {@link AsakusafwOrganizerProfile} for M3BP environment.
 */
class AsakusaM3bpOrganizer extends AbstractOrganizer {

    private final AsakusafwOrganizerM3bpExtension extension

    /**
     * Creates a new instance.
     * @param project the current project
     * @param profile the target profile
     */
    AsakusaM3bpOrganizer(Project project,
            AsakusafwOrganizerProfile profile,
            AsakusafwOrganizerM3bpExtension extension) {
        super(project, profile)
        this.extension = extension
    }

    /**
     * Configures the target profile.
     */
    @Override
    void configureProfile() {
        configureConfigurations()
        configureDependencies()
        configureTasks()
        enableTasks()
    }

    private void configureConfigurations() {
        createConfigurations('asakusafw', [
            M3bpDist : "Contents of Asakusa on M3BP modules (${profile.name}).",
            M3bpNative : "Contents of Asakusa on M3BP native modules (${profile.name}).",
            M3bpNativeDependencies : "Contents of Asakusa on M3BP native dependencies (${profile.name}).",
            M3bpLib : "Libraries of Asakusa on M3BP modules (${profile.name}).",
        ])
    }

    private void configureDependencies() {
        PluginUtils.afterEvaluate(project) {
            AsakusafwBaseExtension base = AsakusafwBasePlugin.get(project)
            AsakusaM3bpBaseExtension m3bp = AsakusaM3bpBasePlugin.get(project)
            createDependencies('asakusafw', [
                M3bpDist : [
                    "com.asakusafw.m3bp.bridge:asakusa-m3bp-assembly:${m3bp.featureVersion}:bootstrap@jar",
                    "com.asakusafw.m3bp.bridge:asakusa-m3bp-bootstrap:${m3bp.featureVersion}:dist@jar",
                ],
                M3bpLib : [
                    "com.asakusafw.m3bp.bridge:asakusa-m3bp-assembly:${m3bp.featureVersion}:lib@jar",
                    "com.asakusafw.m3bp.bridge:asakusa-m3bp-bootstrap:${m3bp.featureVersion}:exec@jar",
                    "ch.qos.logback:logback-classic:${base.logbackVersion}",
                ],
                M3bpNative : [
                    "com.asakusafw.m3bp.bridge:asakusa-m3bp-runtime:${m3bp.featureVersion}:native@jar"
                ],
                M3bpNativeDependencies : [
                    "com.asakusafw.m3bp.bridge:asakusa-m3bp-assembly:${m3bp.featureVersion}:native-dependencies@jar",
                ],
            ])
        }
    }

    private void createDependency(String configurationName, Object notation, Closure<?> configurator) {
        project.dependencies.add(qualify(configurationName), notation, configurator)
    }

    private void configureTasks() {
        createAttachComponentTasks 'attachComponent', [
            M3bp : {
                into('.') {
                    extract configuration('asakusafwM3bpDist')
                    process {
                        filesMatching('**/m3bp/bin/execute') { FileCopyDetails f ->
                            f.setMode(0755)
                        }
                    }
                }
                into('m3bp/lib') {
                    put configuration('asakusafwM3bpLib')
                    process {
                        rename(/(asakusa-m3bp-bootstrap)-.*-exec\.jar/, '$1.jar')
                    }
                }
            },
            M3bpNative : {
                into('.') {
                    extract configuration('asakusafwM3bpNative')
                }
            },
            M3bpNativeDependencies : {
                into('.') {
                    extract configuration('asakusafwM3bpNativeDependencies')
                }
            },
        ]
        createAttachComponentTasks 'attach', [
            M3bpBatchapps : {
                into('batchapps') {
                    put project.asakusafw.m3bp.outputDirectory
                }
            },
        ]
    }

    private void enableTasks() {
        PluginUtils.afterEvaluate(project) {
            if (extension.isEnabled()) {
                project.logger.info "Enabling Asakusa on M3BP (${profile.name})"
                task('attachAssemble').dependsOn task('attachComponentM3bp')
                if (extension.isNativeEnabled()) {
                    project.logger.info "Enabling Asakusa on M3BP native modules (${profile.name})"
                    task('attachAssemble').dependsOn task('attachComponentM3bpNative')
                    if (!extension.isUseSystemNativeDependencies()) {
                        project.logger.info "Enabling Asakusa on M3BP native dependencies (${profile.name})"
                        task('attachAssemble').dependsOn task('attachComponentM3bpNativeDependencies')
                    }
                }
                PluginUtils.afterTaskEnabled(project, AsakusaM3bpSdkPlugin.TASK_COMPILE) { Task compiler ->
                    task('attachM3bpBatchapps').dependsOn compiler
                    if (profile.batchapps.isEnabled()) {
                        project.logger.info "Enabling M3BP Batchapps (${profile.name})"
                        task('attachAssemble').dependsOn task('attachM3bpBatchapps')
                    }
                }
            }
        }
    }
}
