/**
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
package com.asakusafw.m3bp.compiler.inspection.cli;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import com.asakusafw.lang.compiler.common.testing.FileDeployer;
import com.asakusafw.lang.inspection.json.JsonInspectionNodeRepository;
import com.asakusafw.m3bp.compiler.inspection.GraphSpecView;
import com.asakusafw.m3bp.compiler.inspection.cli.Cli.Configuration;

/**
 * Test for {@link Cli}.
 */
public class CliTest {

    /**
     * deployer.
     */
    @Rule
    public FileDeployer deployer = new FileDeployer();

    /**
     * parse - simple case.
     * @throws Exception if failed
     */
    @Test
    public void parse() throws Exception {
        File file = deployer.copy("basic.json", "basic.json");
        Configuration conf = Cli.parse(new String[] {
                "--input", file.getPath(),
        });
        assertThat(conf.input.getCanonicalFile(), is(file.getCanonicalFile()));
        assertThat(conf.vertexId, is(nullValue()));
    }

    /**
     * parse - all opts.
     * @throws Exception if failed
     */
    @Test
    public void parse_opts() throws Exception {
        File file = deployer.copy("basic.json", "basic.json");
        Configuration conf = Cli.parse(new String[] {
                "--input", file.getPath(),
                "--vertex", "v0",
        });
        assertThat(conf.input.getCanonicalFile(), is(file.getCanonicalFile()));
        assertThat(conf.vertexId, is("v0"));
    }

    /**
     * load graph - json.
     * @throws Exception if failed
     */
    @Test
    public void load_json() throws Exception {
        File file = deployer.copy("basic.json", "basic.json");
        GraphSpecView graph = Cli.load(file, new JsonInspectionNodeRepository());
        assertThat(graph.getVertices(), hasSize(greaterThan(0)));
    }

    /**
     * load graph - jobflow lib.
     * @throws Exception if failed
     */
    @Test
    public void load_jobflow() throws Exception {
        File file = deployer.copy("basic.jar", "basic.jar");
        GraphSpecView graph = Cli.load(file, new JsonInspectionNodeRepository());
        assertThat(graph.getVertices(), hasSize(greaterThan(0)));
    }

    /**
     * exec.
     * @throws Exception if failed
     */
    @Test
    public void execute() throws Exception {
        File file = deployer.copy("basic.json", "basic.json");
        int result = Cli.execute(new String[] {
                "--input", file.getPath(),
        });
        assertThat(result, is(0));
    }
}
