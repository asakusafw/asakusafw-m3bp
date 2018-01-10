/**
 * Copyright 2011-2018 Asakusa Framework Team.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import com.asakusafw.m3bp.mirror.ConfigurationMirror.AffinityMode;
import com.asakusafw.m3bp.mirror.EngineMirror;

/**
 * Test for {@link ConfigurationMirrorImpl}.
 */
public class ConfigurationMirrorImplTest {
    /**
     * Checks native library is enabled.
     */
    @ClassRule
    public static final NativeEnabled NATIVE = new NativeEnabled();

    /**
     * Provides test target object.
     */
    @Rule
    public final TestRule preparator = (base, desc) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            try (EngineMirror engine = new EngineMirrorImpl(null)) {
                conf = (ConfigurationMirrorImpl) engine.getConfiguration();
            }
        }
    };

    ConfigurationMirrorImpl conf;

    /**
     * max concurrency.
     * @throws Exception if failed
     */
    @Test
    public void max_concurrency() throws Exception {
        assertThat(conf.withMaxConcurrency(11).getMaxConcurrency(), is(11));
    }

    /**
     * partition count.
     * @throws Exception if failed
     */
    @Test
    public void partition_count() throws Exception {
        assertThat(conf.withPartitionCount(13).getPartitionCount(), is(13));
    }

    /**
     * output buffer size.
     * @throws Exception if failed
     */
    @Test
    public void output_buffer_size() throws Exception {
        assertThat(conf.withOutputBufferSize(17_000_000L).getOutputBufferSize(), is(17_000_000L));
    }

    /**
     * output buffer size.
     * @throws Exception if failed
     */
    @Test
    public void record_count() throws Exception {
        assertThat(conf.withOutputRecordsPerBuffer(23_000L).getOutputRecordsPerBuffer(), is(23_000L));
    }

    /**
     * affinity mode.
     * @throws Exception if failed
     */
    @Test
    public void affinity() throws Exception {
        assertThat(conf.withAffinityMode(AffinityMode.COMPACT).getAffinityMode(), is(AffinityMode.COMPACT));
    }
}
