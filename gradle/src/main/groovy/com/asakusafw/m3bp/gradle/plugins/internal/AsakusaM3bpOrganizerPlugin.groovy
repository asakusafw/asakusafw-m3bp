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
package com.asakusafw.m3bp.gradle.plugins.internal

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import com.asakusafw.gradle.plugins.AsakusafwOrganizerPlugin
import com.asakusafw.gradle.plugins.AsakusafwOrganizerPluginConvention
import com.asakusafw.gradle.plugins.AsakusafwOrganizerProfile
import com.asakusafw.gradle.plugins.internal.PluginUtils
import com.asakusafw.m3bp.gradle.plugins.AsakusafwOrganizerM3bpExtension

/**
 * A Gradle sub plug-in for Asakusa on M3BP project organizer.
 */
class AsakusaM3bpOrganizerPlugin implements Plugin<Project> {

    private Project project

    private NamedDomainObjectCollection<AsakusaM3bpOrganizer> organizers

    @Override
    void apply(Project project) {
        this.project = project
        this.organizers = project.container(AsakusaM3bpOrganizer)

        project.apply plugin: 'asakusafw-organizer'
        project.apply plugin: AsakusaM3bpBasePlugin

        configureConvention()
        configureProfiles()
        configureTasks()
    }

    /**
     * Returns the organizers for each profile (only for testing).
     * @return the organizers for each profile
     */
    NamedDomainObjectCollection<AsakusaM3bpOrganizer> getOrganizers() {
        return organizers
    }

    private void configureConvention() {
        AsakusaM3bpBaseExtension base = AsakusaM3bpBasePlugin.get(project)
        AsakusafwOrganizerPluginConvention convention = project.asakusafwOrganizer
        convention.extensions.create('m3bp', AsakusafwOrganizerM3bpExtension)
        convention.m3bp.conventionMapping.with {
            enabled = { true }
            nativeEnabled = { true }
            useSystemNativeDependencies = { false }
            useSystemHadoop = { false }
        }
        PluginUtils.injectVersionProperty(convention.m3bp, { base.featureVersion })
    }

    private void configureProfiles() {
        AsakusafwOrganizerPluginConvention convention = project.asakusafwOrganizer
        convention.profiles.all { AsakusafwOrganizerProfile profile ->
            configureProfile(profile)
        }
    }

    private void configureProfile(AsakusafwOrganizerProfile profile) {
        AsakusaM3bpBaseExtension base = AsakusaM3bpBasePlugin.get(project)
        AsakusafwOrganizerM3bpExtension extension = profile.extensions.create('m3bp', AsakusafwOrganizerM3bpExtension)
        AsakusafwOrganizerM3bpExtension parent = project.asakusafwOrganizer.m3bp
        extension.conventionMapping.with {
            enabled = { parent.enabled }
            nativeEnabled = { parent.nativeEnabled }
            useSystemNativeDependencies = { parent.useSystemNativeDependencies }
            useSystemHadoop = { parent.useSystemHadoop }
        }
        PluginUtils.injectVersionProperty(extension, { base.featureVersion })

        AsakusaM3bpOrganizer organizer = new AsakusaM3bpOrganizer(project, profile, extension)
        organizer.configureProfile()
        organizers << organizer
    }

    private void configureTasks() {
        defineFacadeTasks([
            attachComponentM3bp : 'Attaches Asakusa on M3BP components to assemblies.',
            attachM3bpBatchapps : 'Attaches Asakusa on M3BP batch applications to assemblies.',
        ])
    }

    private void defineFacadeTasks(Map<String, String> taskMap) {
        taskMap.each { String taskName, String desc ->
            project.task(taskName) { Task task ->
                if (desc != null) {
                    task.group AsakusafwOrganizerPlugin.ASAKUSAFW_ORGANIZER_GROUP
                    task.description desc
                }
                organizers.all { AsakusaM3bpOrganizer organizer ->
                    task.dependsOn organizer.task(task.name)
                }
            }
        }
    }
}
