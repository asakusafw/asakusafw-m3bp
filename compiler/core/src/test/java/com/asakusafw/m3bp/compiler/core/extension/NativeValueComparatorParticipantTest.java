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
package com.asakusafw.m3bp.compiler.core.extension;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

import com.asakusafw.dag.compiler.codegen.NativeValueComparatorExtension;
import com.asakusafw.dag.runtime.io.ValueOptionSerDe;
import com.asakusafw.dag.runtime.testing.MockDataModel;
import com.asakusafw.dag.utils.common.Action;
import com.asakusafw.lang.compiler.api.CompilerOptions;
import com.asakusafw.lang.compiler.api.testing.MockDataModelLoader;
import com.asakusafw.lang.compiler.model.description.Descriptions;
import com.asakusafw.lang.compiler.model.graph.Group;
import com.asakusafw.lang.compiler.model.graph.Groups;
import com.asakusafw.lang.compiler.packaging.FileContainer;
import com.asakusafw.m3bp.compiler.common.CommandPath;
import com.asakusafw.m3bp.mirror.jna.BufferComparator;
import com.asakusafw.m3bp.mirror.jna.NativeFunctionLoader;
import com.asakusafw.runtime.value.IntOption;
import com.asakusafw.runtime.value.ValueOption;

/**
 * Test for {@link NativeValueComparatorParticipant}.
 */
public class NativeValueComparatorParticipantTest {

    /**
     * temporary folder.
     */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /**
     * path helper.
     */
    @Rule
    public final ExternalResource helper = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            CommandPath path = CommandPath.system();
            if (isReady(path) == false) {
                path = path.append(new CommandPath(new File("/usr/local/bin")));
                Assume.assumeThat(isReady(path), is(true));
                options.withProperty(NativeValueComparatorParticipant.KEY_PATH, path.asPathString());
            }
        }
        private boolean isReady(CommandPath path) {
            return path.find(NativeValueComparatorParticipant.DEFAULT_CMAKE) != null
                    && path.find(NativeValueComparatorParticipant.DEFAULT_MAKE) != null;
        }
    };

    final CompilerOptions.Builder options = CompilerOptions.builder();

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        loading(MockDataModel.class, group("+key"), c -> {
            ByteBuffer a = serialize(new IntOption(100));
            ByteBuffer b = serialize(new IntOption(100));
            ByteBuffer x = serialize(new IntOption(101));
            assertThat(comparing(c, a, b), is(0));
            assertThat(comparing(c, a, x), is(lessThan(0)));
        });
    }

    private static Group group(String... expressions) {
        List<String> grouping = new ArrayList<>();
        List<String> ordering = new ArrayList<>();
        for (String s : expressions) {
            if (s.startsWith("=")) {
                grouping.add(s.substring(1));
            } else {
                ordering.add(s);
            }
        }
        return Groups.parse(grouping, ordering);
    }

    private int comparing(BufferComparator comp, ByteBuffer a, ByteBuffer b) {
        if (comp.compare(a, b)) {
            return -1;
        } else if (comp.compare(b, a)) {
            return +1;
        } else {
            return 0;
        }
    }

    private ByteBuffer serialize(ValueOption<?>... values) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(output)) {
            for (ValueOption<?> value : values) {
                ValueOptionSerDe.serializeAny(value, out);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        byte[] bytes = output.toByteArray();
        ByteBuffer buf = ByteBuffer.allocateDirect(bytes.length);
        buf.put(bytes);
        buf.flip();
        return buf;
    }

    private void loading(Class<?> type, Group group, Action<BufferComparator, Exception> action) {
        AtomicReference<String> name = new AtomicReference<>();
        FileContainer dst = building(e -> {
            name.set(e.addComparator(Descriptions.typeOf(type), group));
        });
        loading(dst, l -> {
            try {
                BufferComparator comparator = l.getComparator(name.get());
                action.perform(comparator);
            } catch (UnsatisfiedLinkError e) {
                throw new AssertionError(e);
            }
        });
    }

    private FileContainer building(Action<NativeValueComparatorExtension, Exception> action) {
        try {
            FileContainer src = new FileContainer(temporary.newFolder());
            FileContainer dst = new FileContainer(temporary.newFolder());
            NativeValueComparatorExtension extension = NativeValueComparatorParticipant.createExtension(
                    options.build(),
                    src,
                    new MockDataModelLoader(getClass().getClassLoader()));
            action.perform(extension);
            NativeValueComparatorParticipant.processExtension(extension, dst);
            return dst;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void loading(FileContainer dst, Action<NativeFunctionLoader, Exception> action) {
        String libname = System.mapLibraryName(NativeValueComparatorParticipant.ARTIFACCT_LIB_NAME);
        File file = dst.toFile(NativeValueComparatorParticipant.PATH_OUTPUT_BASE.append(libname));
        try (NativeFunctionLoader loader = new NativeFunctionLoader(file)) {
            action.perform(loader);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
