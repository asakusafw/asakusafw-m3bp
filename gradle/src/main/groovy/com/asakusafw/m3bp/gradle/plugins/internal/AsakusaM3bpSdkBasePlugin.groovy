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

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.asakusafw.gradle.plugins.AsakusafwSdkExtension
import com.asakusafw.gradle.plugins.internal.AsakusaSdkPlugin
import com.asakusafw.gradle.plugins.internal.PluginUtils
import com.asakusafw.lang.gradle.plugins.internal.AsakusaLangSdkPlugin

/**
 * A base plug-in of {@link AsakusaM3bpSdkPlugin}.
 * This only organizes dependencies and testkits.
 * @since 0.3.0
 */
class AsakusaM3bpSdkBasePlugin implements Plugin<Project> {

    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.apply plugin: AsakusaLangSdkPlugin
        project.apply plugin: AsakusaM3bpBasePlugin

        configureTestkit()
        configureConfigurations()
    }

    private void configureTestkit() {
        AsakusafwSdkExtension sdk = AsakusaSdkPlugin.get(project).sdk
        sdk.availableTestkits << new AsakusaM3bpTestkit()
    }

    private void configureConfigurations() {
        project.configurations {
            asakusaM3bpCommon {
                description 'Common libraries of Asakusa DSL Compiler for M3BP'
                extendsFrom project.configurations.asakusaLangCommon
                exclude group: 'asm', module: 'asm'
            }
            asakusaM3bpCompiler {
                description 'Full classpath of Asakusa DSL Compiler for M3BP'
                extendsFrom project.configurations.asakusaLangCompiler
                extendsFrom project.configurations.asakusaM3bpCommon
            }
            asakusaM3bpTestkit {
                description 'Asakusa DSL testkit classpath for M3BP'
                extendsFrom project.configurations.asakusaLangTestkit
                extendsFrom project.configurations.asakusaM3bpCommon
            }
        }
        PluginUtils.afterEvaluate(project) {
            AsakusaM3bpBaseExtension base = AsakusaM3bpBasePlugin.get(project)
            AsakusafwSdkExtension features = AsakusaSdkPlugin.get(project).sdk
            project.dependencies {
                if (features.core) {
                    asakusaM3bpCommon "com.asakusafw.m3bp.compiler:asakusa-m3bp-compiler-core:${base.featureVersion}"
                    if (features.directio) {
                        asakusaM3bpCommon "com.asakusafw.dag.compiler:asakusa-dag-compiler-extension-directio:${base.langVersion}"
                    }
                    if (features.windgate) {
                        asakusaM3bpCommon "com.asakusafw.dag.compiler:asakusa-dag-compiler-extension-windgate:${base.langVersion}"
                    }
                }
                if (features.testing) {
                    asakusaM3bpTestkit "com.asakusafw.m3bp.compiler:asakusa-m3bp-compiler-test-adapter:${base.featureVersion}"
                    asakusaM3bpTestkit "com.asakusafw.m3bp.bridge:asakusa-m3bp-assembly:${base.featureVersion}"
                }
            }
        }
    }
}
