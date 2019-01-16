/*
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
package com.asakusafw.m3bp.gradle.plugins.internal

import java.util.concurrent.Callable

import org.gradle.api.Buildable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import com.asakusafw.gradle.plugins.AsakusafwCompilerExtension
import com.asakusafw.gradle.plugins.AsakusafwPluginConvention
import com.asakusafw.gradle.tasks.AsakusaCompileTask
import com.asakusafw.gradle.tasks.internal.ResolutionUtils
import com.asakusafw.lang.gradle.plugins.internal.AsakusaLangSdkPlugin

/**
 * Test for {@link AsakusaM3bpSdkPlugin}.
 */
class AsakusaM3bpSdkPluginTest {

    /**
     * The test initializer.
     */
    @Rule
    public final TestRule initializer = new TestRule() {
        Statement apply(Statement stmt, Description desc) {
            project = ProjectBuilder.builder().withName(desc.methodName).build()
            project.apply plugin: AsakusaM3bpSdkPlugin
            return stmt
        }
    }

    Project project

    /**
     * test for base plug-ins.
     */
    @Test
    void base() {
        assert project.plugins.hasPlugin('asakusafw-sdk') != null
        assert project.plugins.hasPlugin(AsakusaM3bpBasePlugin) != null
        assert project.plugins.hasPlugin(AsakusaLangSdkPlugin) != null
    }

    /**
     * test for extension.
     */
    @Test
    void extension() {
        AsakusafwPluginConvention root = project.asakusafw
        AsakusafwCompilerExtension extension = root.m3bp
        assert extension != null

        assert project.file(extension.outputDirectory) == project.file("${project.buildDir}/m3bp-batchapps")
        assert extension.include == null
        assert extension.exclude == null

        assert extension.runtimeWorkingDirectory == null

        assert extension.batchIdPrefix == 'm3bp.'
        assert extension.failOnError == true

        Map<String, String> props = ResolutionUtils.resolveToStringMap(extension.compilerProperties)
        assert props['javac.version'] == root.javac.sourceCompatibility.toString()
        assert props['redirector.rule.com.asakusafw.runtime.core.BatchContext'] == 'com.asakusafw.m3bp.custom.M3bpBatchContext'
        assert props['redirector.rule.com.asakusafw.runtime.core.Report'] == 'com.asakusafw.bridge.api.Report'
        assert props['redirector.rule.com.asakusafw.runtime.directio.api.DirectIo'] == 'com.asakusafw.bridge.directio.api.DirectIo'
    }

    /**
     * test for version.
     */
    @Test
    void extension_version() {
        project.asakusaM3bpBase.featureVersion = '__VERSION__'
        assert project.asakusafw.m3bp.version == '__VERSION__'
    }

    /**
     * test for {@code tasks.m3bpCompileBatchapps}.
     */
    @Test
    void tasks_m3bpCompileBatchapps() {
        AsakusaCompileTask task = project.tasks.m3bpCompileBatchapps
        assert task != null
        assert task.group != null
        assert task.description != null

        AsakusafwPluginConvention root = project.asakusafw
        AsakusafwCompilerExtension extension = root.m3bp

        root.maxHeapSize = '123m'
        assert task.maxHeapSize == root.maxHeapSize

        assert task.toolClasspath.empty == false

        assert task.explore.empty == false
        assert task.attach.empty == false
        assert task.embed.empty == false
        assert task.external.empty

        extension.include = null
        assert task.resolvedInclude.empty

        extension.include = 'include.*'
        assert task.resolvedInclude == [extension.include]

        extension.include = ['include1.*', 'include2.*']
        assert task.resolvedInclude.toSet() == extension.include.toSet()

        extension.exclude = null
        assert task.resolvedExclude.empty

        extension.exclude = 'exclude.*'
        assert task.resolvedExclude == [extension.exclude]

        extension.exclude = ['exclude1.*', 'exclude2.*']
        assert task.resolvedExclude.toSet() == extension.exclude.toSet()

        extension.runtimeWorkingDirectory = null
        assert task.runtimeWorkingDirectory == null

        extension.runtimeWorkingDirectory = 'RWD'
        assert task.runtimeWorkingDirectory == extension.runtimeWorkingDirectory

        // NOTE: 'task.compilerProperties' will be propagated in 'project.afterEvaluate'
        // m3bp.compilerProperties.put('TESTING', 'OK')
        // assert task.resolvedCompilerProperties.get('TESTING') == 'OK'

        extension.batchIdPrefix = null
        assert task.batchIdPrefix == null

        extension.batchIdPrefix = 'tprefix.'
        assert task.batchIdPrefix == extension.batchIdPrefix

        extension.outputDirectory = 'testing/batchapps'
        assert task.outputDirectory.canonicalFile == project.file(extension.outputDirectory).canonicalFile

        extension.failOnError = false
        assert task.failOnError == extension.failOnError
    }

    /**
     * Test for {@code project.tasks.compileBatchapp}.
     */
    @Test
    void tasks_compileBatchapp() {
        Task task = project.tasks.findByName('compileBatchapp')
        assert task != null
        assert dependencies(task).contains('m3bpCompileBatchapps')
    }

    Set<String> dependencies(Task task) {
        return task.getDependsOn().collect { toTaskNames(task, it) }.flatten().toSet()
    }

    Collection<String> toTaskNames(Task origin, Object value) {
        if (value instanceof Task) {
            return [ value.name ]
        } else if (value instanceof Callable<?>) {
            return toTaskNames(origin, value.call() ?: [])
        } else if (value instanceof TaskDependency) {
            return value.getDependencies(origin).collect { it.name }
        } else if (value instanceof Buildable) {
            return toTaskNames(origin, value.buildDependencies)
        } else if (value instanceof Collection<?> || value instanceof Object[]) {
            return value.collect { toTaskNames(origin, it) }.flatten()
        } else {
            return [ String.valueOf(value) ]
        }
    }
}
