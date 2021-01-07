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
package com.asakusafw.m3bp.mirror.jna;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.sun.jna.NativeLibrary;

/**
 * Test for {@link NativeFunctionLoader}.
 */
public class NativeFunctionLoaderTest {

    static final File NATIVE_DIR = new File("target/native/test/lib");

    static final String LIBRARY_NAME = "testing-" + NativeFunctionLoader.class.getSimpleName();

    /**
     * Initializes/finalizes {@link NativeFunctionLoader}.
     */
    @ClassRule
    public static final ExternalResource LOADER_LIFECYCLE = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            try {
                if (NATIVE_DIR.isDirectory()) {
                    NativeLibrary.addSearchPath(LIBRARY_NAME, NATIVE_DIR.getAbsolutePath());
                }
                NativeFunctionLoaderTest.loader = new NativeFunctionLoader(LIBRARY_NAME);
            } catch (UnsatisfiedLinkError e) {
                Assume.assumeNoException("native library is not available", e);
            }
        }
        @Override
        protected void after() {
            if (loader != null) {
                loader.close();
                loader = null;
            }
        }
    };

    static NativeFunctionLoader loader;

    /**
     * simple case.
     */
    @Test
    public void simple() {
        ByteBuffer a = alloc(Integer.BYTES);
        ByteBuffer b = alloc(Integer.BYTES);
        a.putInt(0).flip();
        b.putInt(0).flip();
        BufferComparator cmp = loader.getComparator("lt_int32");
        assertThat(cmp.compare(a, b), is(false));
        assertThat(cmp.compare(b, a), is(false));
    }

    /**
     * {@code a < b}.
     */
    @Test
    public void lt() {
        ByteBuffer a = alloc(Integer.BYTES);
        ByteBuffer b = alloc(Integer.BYTES);
        a.putInt(0).flip();
        b.putInt(1).flip();
        BufferComparator cmp = loader.getComparator("lt_int32");
        assertThat(cmp.compare(a, b), is(true));
        assertThat(cmp.compare(b, a), is(false));
    }

    private ByteBuffer alloc(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }
}
