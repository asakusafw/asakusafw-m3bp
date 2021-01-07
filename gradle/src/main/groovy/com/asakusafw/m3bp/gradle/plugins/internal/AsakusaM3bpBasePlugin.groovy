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

import com.asakusafw.gradle.plugins.AsakusafwBasePlugin

/**
 * A Gradle sub plug-in for Asakusa on M3BP compiler.
 */
class AsakusaM3bpBasePlugin implements Plugin<Project> {

    private static final String ARTIFACT_INFO_PATH = 'META-INF/asakusa-m3bp-gradle/artifact.properties'

    private static final String INVALID_VERSION = 'INVALID'

    private Project project

    private AsakusaM3bpBaseExtension extension

    /**
     * Applies this plug-in and returns the extension object for the project.
     * @param project the target project
     * @return the corresponded extension
     */
    static AsakusaM3bpBaseExtension get(Project project) {
        project.apply plugin: AsakusaM3bpBasePlugin
        return project.plugins.getPlugin(AsakusaM3bpBasePlugin).extension
    }

    @Override
    void apply(Project project) {
        this.project = project
        project.apply plugin: AsakusafwBasePlugin

        this.extension = project.extensions.create('asakusaM3bpBase', AsakusaM3bpBaseExtension)
        configureExtension()
        configureTasks()
    }

    private void configureExtension() {
        configureArtifactVersions()
    }

    private void configureArtifactVersions() {
        Properties properties = loadProperties(ARTIFACT_INFO_PATH)
        driveProperties(ARTIFACT_INFO_PATH, [
            'feature-version': 'Asakusa on M3BP',
            'core-version': 'Asakusa Core libraries',
            'lang-version': 'Asakusa DSL compiler',
            'hadoop-version': 'Hadoop',
        ])
        project.logger.info "Asakusa on M3BP: ${extension.featureVersion}"
    }

    private void driveProperties(String path, Map<String, String> configurations) {
        Properties properties = loadProperties(path)
        configurations.each { String key, String name ->
            StringBuilder buf = new StringBuilder()
            boolean sawHyphen = false
            for (char c : key.toCharArray()) {
                if (c == '-') {
                    sawHyphen = true
                } else {
                    buf.append(sawHyphen ? Character.toUpperCase(c) : c)
                    sawHyphen = false
                }
            }
            String prop = buf.toString()
            assert extension.hasProperty(prop)
            extension[prop] = extract(properties, key, name)
        }
    }

    private String extract(Properties properties, String key, String name) {
        String value = properties.getProperty(key, INVALID_VERSION)
        if (value == INVALID_VERSION) {
            project.logger.warn "failed to detect version of ${name}"
        } else {
            project.logger.info "${name} version: ${value}"
        }
        return value
    }

    private Properties loadProperties(String path) {
        Properties results = new Properties()
        InputStream input = getClass().classLoader.getResourceAsStream(path)
        if (input == null) {
            project.logger.warn "missing properties file: ${path}"
        } else {
            try {
                results.load(input)
            } catch (IOException e) {
                project.logger.warn "error occurred while extracting properties: ${path}"
            } finally {
                input.close()
            }
        }
        return results
    }

    private void configureTasks() {
        extendVersionsTask()
    }

    private void extendVersionsTask() {
        project.tasks.getByName(AsakusafwBasePlugin.TASK_VERSIONS).doLast {
            logger.lifecycle "Asakusa on M3BP: ${extension.featureVersion}"
        }
    }
}
