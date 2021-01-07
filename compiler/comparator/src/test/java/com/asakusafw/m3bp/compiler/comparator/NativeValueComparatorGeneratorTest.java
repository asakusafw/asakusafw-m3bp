/**
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
package com.asakusafw.m3bp.compiler.comparator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.asakusafw.lang.compiler.common.BasicResourceContainer;
import com.asakusafw.lang.compiler.common.Location;

/**
 * Test for {@link NativeValueComparatorGenerator}.
 */
public class NativeValueComparatorGeneratorTest {

    /**
     * A temporary folder.
     */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /**
     * copying header files.
     */
    @Test
    public void copy_headers() {
        Location base = Location.of("include");
        BasicResourceContainer c = new BasicResourceContainer(temporary.getRoot());
        NativeValueComparatorGenerator.copyHeaderFiles(c, base);
        for (Location name : NativeValueComparatorGenerator.HEADER_FILE_NAMES) {
            File f = c.toFile(base.append(name));
            assertThat(f.isFile(), is(true));
        }
    }

    /**
     * copying source files.
     */
    @Test
    public void copy_sources() {
        Location base = Location.of("include");
        BasicResourceContainer c = new BasicResourceContainer(temporary.getRoot());
        NativeValueComparatorGenerator.copySourceFiles(c, base);
        for (Location name : NativeValueComparatorGenerator.SOURCE_FILE_NAMES) {
            File f = c.toFile(base.append(name));
            assertThat(f.isFile(), is(true));
        }
    }
}
