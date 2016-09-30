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
package com.asakusafw.m3bp.gradle.plugins.internal

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.asakusafw.gradle.plugins.AsakusafwCompilerExtension
import com.asakusafw.gradle.plugins.AsakusafwPluginConvention
import com.asakusafw.gradle.plugins.AsakusafwSdkExtension
import com.asakusafw.gradle.plugins.internal.AsakusaSdkPlugin
import com.asakusafw.gradle.plugins.internal.PluginUtils

/**
 * A base plug-in of {@link AsakusaM3bpSdkPlugin}.
 * This only organizes conventions and dependencies.
 * @since 0.3.0
 */
class AsakusaM3bpSdkBasePlugin implements Plugin<Project> {

    private static final Map<String, String> REDIRECT = [
            'com.asakusafw.runtime.core.BatchContext' : 'com.asakusafw.m3bp.custom.M3bpBatchContext',
            'com.asakusafw.runtime.core.Report' : 'com.asakusafw.bridge.api.Report',
            'com.asakusafw.runtime.directio.api.DirectIo' : 'com.asakusafw.bridge.directio.api.DirectIo',
    ]

    private Project project

    private AsakusafwCompilerExtension extension

    @Override
    void apply(Project project) {
        this.project = project

        project.apply plugin: AsakusaSdkPlugin
        project.apply plugin: AsakusaM3bpBasePlugin
        extension = AsakusaSdkPlugin.get(project).extensions.create('m3bp', AsakusafwCompilerExtension)

        configureExtension()
        configureConfigurations()
    }

    private void configureExtension() {
        AsakusaM3bpBaseExtension base = AsakusaM3bpBasePlugin.get(project)
        AsakusafwPluginConvention sdk = AsakusaSdkPlugin.get(project)
        extension.conventionMapping.with {
            outputDirectory = { project.relativePath(new File(project.buildDir, 'm3bp-batchapps')) }
            batchIdPrefix = { (String) 'm3bp.' }
            failOnError = { true }
        }
        REDIRECT.each { k, v ->
            extension.compilerProperties.put((String) "redirector.rule.${k}", v)
        }
        extension.compilerProperties.put('javac.version', { sdk.javac.sourceCompatibility.toString() })
        PluginUtils.injectVersionProperty(extension, { base.featureVersion })
        sdk.sdk.availableTestkits << new AsakusaM3bpTestkit()
    }

    private void configureConfigurations() {
        project.configurations {
            asakusaM3bpCommon {
                description 'Common libraries of Asakusa DSL Compiler for M3BP'
                exclude group: 'asm', module: 'asm'
            }
            asakusaM3bpCompiler {
                description 'Full classpath of Asakusa DSL Compiler for M3BP'
                extendsFrom project.configurations.compile
                extendsFrom project.configurations.asakusaM3bpCommon
            }
            asakusaM3bpTestkit {
                description 'Asakusa DSL testkit classpath for M3BP'
                extendsFrom project.configurations.asakusaM3bpCommon
            }
        }
        PluginUtils.afterEvaluate(project) {
            AsakusaM3bpBaseExtension base = AsakusaM3bpBasePlugin.get(project)
            AsakusafwPluginConvention sdk = AsakusaSdkPlugin.get(project)
            AsakusafwSdkExtension features = sdk.sdk
            project.dependencies {
                if (features.core) {
                    asakusaM3bpCommon "com.asakusafw.m3bp.compiler:asakusa-m3bp-compiler-core:${base.featureVersion}"
                    asakusaM3bpCommon "com.asakusafw.lang.compiler:asakusa-compiler-cli:${base.langVersion}"
                    asakusaM3bpCommon "com.asakusafw.lang.compiler:asakusa-compiler-extension-redirector:${base.langVersion}"
                    asakusaM3bpCommon "com.asakusafw.lang.compiler:asakusa-compiler-extension-yaess:${base.langVersion}"
                    asakusaM3bpCommon "com.asakusafw:simple-graph:${sdk.asakusafwVersion}"
                    asakusaM3bpCommon "com.asakusafw:java-dom:${sdk.asakusafwVersion}"

                    asakusaM3bpCompiler "com.asakusafw:asakusa-dsl-vocabulary:${sdk.asakusafwVersion}"
                    asakusaM3bpCompiler "com.asakusafw:asakusa-runtime:${sdk.asakusafwVersion}"
                    asakusaM3bpCompiler "com.asakusafw:asakusa-yaess-core:${sdk.asakusafwVersion}"

                    if (features.directio) {
                        asakusaM3bpCommon "com.asakusafw.dag.compiler:asakusa-dag-compiler-extension-directio:${base.langVersion}"
                        asakusaM3bpCompiler "com.asakusafw:asakusa-directio-vocabulary:${sdk.asakusafwVersion}"
                    }
                    if (features.windgate) {
                        asakusaM3bpCommon "com.asakusafw.dag.compiler:asakusa-dag-compiler-extension-windgate:${base.langVersion}"
                        asakusaM3bpCompiler "com.asakusafw:asakusa-windgate-vocabulary:${sdk.asakusafwVersion}"
                    }
                    if (features.hive) {
                        asakusaM3bpCommon "com.asakusafw.lang.compiler:asakusa-compiler-extension-hive:${base.langVersion}"
                    }
                }
                if (features.testing) {
                    asakusaM3bpTestkit "com.asakusafw.m3bp.compiler:asakusa-m3bp-compiler-test-adapter:${base.featureVersion}"
                    asakusaM3bpTestkit "com.asakusafw.m3bp.bridge:asakusa-m3bp-assembly:${base.featureVersion}"
                }
            }
        }
    }

    /**
     * Returns the extension object of this plug-in.
     * The plug-in will be applied automatically.
     * @param project the target project
     * @return the related extension
     */
    static AsakusafwCompilerExtension get(Project project) {
        project.apply plugin: AsakusaM3bpSdkBasePlugin
        AsakusaM3bpSdkBasePlugin plugin = project.plugins.getPlugin(AsakusaM3bpSdkBasePlugin)
        if (plugin == null) {
            throw new IllegalStateException('AsakusaM3bpSdkBasePlugin has not been applied')
        }
        return plugin.extension
    }
}
