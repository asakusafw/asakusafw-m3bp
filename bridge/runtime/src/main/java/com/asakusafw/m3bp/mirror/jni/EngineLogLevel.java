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
package com.asakusafw.m3bp.mirror.jni;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum EngineLogLevel {

    TRACE(0, Logger::isTraceEnabled),

    DEBUG(1, Logger::isDebugEnabled),

    INFO(2, Logger::isInfoEnabled),

    WARNING(3, Logger::isWarnEnabled),

    ERROR(4, x -> true),

    CRITICAL(5, x -> true),
    ;

    final int id;

    private final Predicate<? super Logger> is;

    EngineLogLevel(int id, Predicate<? super Logger> is) {
        this.id = id;
        this.is = is;
    }

    public static EngineLogLevel get(String name) {
        return get(LoggerFactory.getLogger(name));
    }

    public static EngineLogLevel get(Logger logger) {
        return Stream.of(values())
                .filter(l -> l.is.test(logger))
                .findFirst()
                .orElse(CRITICAL);
    }
}
