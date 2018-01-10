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
package com.asakusafw.m3bp.compiler.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Io;

/**
 * Redirects inputs.
 */
final class ReaderRedirector implements Runnable, Io {

    static final Logger LOG = LoggerFactory.getLogger(ReaderRedirector.class);

    private BufferedReader reader;

    private final Consumer<CharSequence> consumer;

    ReaderRedirector(InputStream input, Consumer<CharSequence> consumer) {
        this(new InputStreamReader(Arguments.requireNonNull(input), Charset.defaultCharset()), consumer);
    }

    ReaderRedirector(Reader reader, Consumer<CharSequence> consumer) {
        Arguments.requireNonNull(reader);
        Arguments.requireNonNull(consumer);
        this.reader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        this.consumer = consumer;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String line;
                synchronized (this) {
                    if (reader == null) {
                        return;
                    } else {
                        line = reader.readLine();
                    }
                }
                if (line == null) {
                    return;
                }
                consumer.accept(line);
            }
        } catch (IOException e) {
            LOG.warn("error occurred while reading output", e);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (reader != null) {
                Reader r = reader;
                reader = null;
                r.close();
            }
        }
    }
}
