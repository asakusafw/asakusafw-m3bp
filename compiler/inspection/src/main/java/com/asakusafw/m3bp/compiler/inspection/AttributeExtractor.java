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
package com.asakusafw.m3bp.compiler.inspection;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Optionals;
import com.asakusafw.dag.utils.common.Tuple;

/**
 * Extracts attributes.
 */
class AttributeExtractor {

    private static final Pattern PATTERN_CORRECTION = Pattern.compile("[\\[\\{](.*)[\\]\\}]");

    private final Map<String, String> properties;

    AttributeExtractor(Map<String, String> properties) {
        Arguments.requireNonNull(properties);
        this.properties = Arguments.copy(properties);
    }

    public Optional<String> extract(String key) {
        return Optionals.remove(properties, key);
    }

    public Optional<List<String>> extractList(String key) {
        return extract(key)
                .flatMap(v -> {
                    Matcher matcher = PATTERN_CORRECTION.matcher(v);
                    if (matcher.matches() == false) {
                        return Optionals.empty();
                    }
                    return Stream.of(matcher.group(1).split("\\,"))
                            .map(String::trim)
                            .filter(s -> s.isEmpty() == false)
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    Optionals::of));
                });
    }

    public Optional<Set<String>> extractSet(String key) {
        return extractList(key).map(LinkedHashSet<String>::new);
    }

    public Map<String, String> extractProperties() {
        Map<String, String> results = new LinkedHashMap<>();
        properties.entrySet().stream()
                .map(e -> new Tuple<>(e.getKey(), e.getValue()))
                .filter(e -> e.left().startsWith(ElementSpecView.KEY_ATTRIBUTE_PREFIX))
                .map(e -> new Tuple<>(e.left().substring(ElementSpecView.KEY_ATTRIBUTE_PREFIX.length()), e.right()))
                .forEachOrdered(e -> results.put(e.left(), e.right()));
        return results;
    }
}
