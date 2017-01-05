/**
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
package com.asakusafw.m3bp.mirror.jni;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates if native library is enabled.
 */
public class NativeEnabled implements TestRule {

    static final Logger LOG = LoggerFactory.getLogger(NativeEnabled.class);

    static final boolean ENABLED;
    static {
        boolean enabled;
        try {
            Class.forName(EngineMirrorImpl.class.getName(), true, EngineMirrorImpl.class.getClassLoader());
            enabled = true;
        } catch (ClassNotFoundException | UnsatisfiedLinkError e) {
            LOG.debug("native library is not enabled", e);
            enabled = false;
        }
        ENABLED = enabled;
    }

    /**
     * Returns whether the native library is enabled or not.
     * @return {@code true} if it is enabled, otherwise {@code false}
     */
    public static boolean isEnabled() {
        return ENABLED;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Assume.assumeTrue(ENABLED);
        return base;
    }
}
