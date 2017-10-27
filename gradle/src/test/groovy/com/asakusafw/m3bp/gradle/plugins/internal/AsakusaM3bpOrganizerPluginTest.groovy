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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import com.asakusafw.gradle.plugins.AsakusafwOrganizerPluginConvention
import com.asakusafw.lang.gradle.plugins.internal.AsakusaLangOrganizerPlugin
import com.asakusafw.m3bp.gradle.plugins.AsakusafwOrganizerM3bpExtension

/**
 * Test for {@link AsakusaM3bpOrganizerPlugin}.
 */
class AsakusaM3bpOrganizerPluginTest {

    /**
     * The test initializer.
     */
    @Rule
    public final TestRule initializer = new TestRule() {
        Statement apply(Statement stmt, Description desc) {
            project = ProjectBuilder.builder().withName(desc.methodName).build()
            project.apply plugin: AsakusaM3bpOrganizerPlugin
            return stmt
        }
    }

    Project project

    /**
     * test for base plug-ins.
     */
    @Test
    void base() {
        assert project.plugins.hasPlugin('asakusafw-organizer') != null
        assert project.plugins.hasPlugin(AsakusaM3bpBasePlugin) != null
        assert project.plugins.hasPlugin(AsakusaLangOrganizerPlugin) != null
    }

    /**
     * test for extension.
     */
    @Test
    void extension() {
        AsakusafwOrganizerPluginConvention root = project.asakusafwOrganizer
        AsakusafwOrganizerM3bpExtension extension = root.m3bp
        assert extension != null

        assert extension.enabled == true
        assert extension.nativeEnabled == true
        assert extension.useSystemNativeDependencies == false

        assert root.profiles.dev.m3bp.enabled == true
        assert root.profiles.prod.m3bp.enabled == true

        root.profiles.testing {
            // ok
        }
        assert root.profiles.testing.m3bp.enabled == true
    }

    /**
     * Test for {@code project.asakusafwOrganizer.m3bp.version}.
     */
    @Test
    void extension_version() {
        project.asakusaM3bpBase.featureVersion = '__VERSION__'
        assert project.asakusafwOrganizer.m3bp.version == '__VERSION__'
        assert project.asakusafwOrganizer.profiles.dev.m3bp.version == '__VERSION__'
        assert project.asakusafwOrganizer.profiles.prod.m3bp.version == '__VERSION__'
        assert project.asakusafwOrganizer.profiles.other.m3bp.version == '__VERSION__'
    }

    /**
     * test for extension.
     */
    @Test
    void extension_inherited() {
        AsakusafwOrganizerPluginConvention root = project.asakusafwOrganizer
        AsakusafwOrganizerM3bpExtension extension = root.m3bp

        extension.enabled = false

        assert root.profiles.dev.m3bp.enabled == false
        assert root.profiles.prod.m3bp.enabled == false

        root.profiles.prod.m3bp.enabled = true
        assert extension.enabled == false
        assert root.profiles.dev.m3bp.enabled == false
        assert root.profiles.prod.m3bp.enabled == true


        root.profiles.testing {
            // ok
        }
        assert root.profiles.testing.m3bp.enabled == false
    }

    /**
     * test for {@code tasks.attachComponentM3bp_*}.
     */
    @Test
    void tasks_attachComponentM3bp() {
        assert project.tasks.findByName('attachComponentM3bp_dev') != null
        assert project.tasks.findByName('attachComponentM3bp_prod') != null

        assert project.tasks.findByName('attachComponentM3bp_testing') == null
        project.asakusafwOrganizer.profiles.testing {
            // ok
        }
        assert project.tasks.findByName('attachComponentM3bp_testing') != null
    }
}
