/**
 * Copyright 2011-2019 Asakusa Framework Team.
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
package com.asakusafw.m3bp.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.bridge.api.BatchContext;
import com.asakusafw.bridge.stage.StageInfo;
import com.asakusafw.lang.utils.common.Arguments;
import com.asakusafw.lang.utils.common.Optionals;
import com.asakusafw.runtime.stage.StageConstants;

/**
 * Custom implementation of {@link BatchContext}.
 */
@SuppressWarnings("deprecation")
public final class M3bpBatchContext {

    static final Logger LOG = LoggerFactory.getLogger(M3bpBatchContext.class);

    private static final AtomicReference<Map<String, String>> VARIABLES = new AtomicReference<>();

    private M3bpBatchContext() {
        return;
    }

    /**
     * Returns a batch argument.
     * @param name the argument name
     * @return the corresponded value, or {@code null} if the target argument is not defined
     * @throws IllegalArgumentException if the parameter is {@code null}
     * @throws IllegalStateException if the current session is wrong
     */
    public static String get(String name) {
        Arguments.requireNonNull(name);
        Map<String, String> variables = VARIABLES.get();
        if (variables == null) {
            return BatchContext.get(name);
        } else {
            LOG.trace("using M3bpBatchContext: {}", name);
            return variables.get(name);
        }
    }

    /**
     * Initializes this class.
     * @param info the current stage information
     * @return {@code true} if actually activated, otherwise {@code false}
     */
    static boolean activate(StageInfo info) {
        Arguments.requireNonNull(info);
        Map<String, String> map = new HashMap<>();
        map.putAll(info.getBatchArguments());
        Optionals.put(map, StageConstants.VAR_USER, info.getUserName());
        Optionals.put(map, StageConstants.VAR_BATCH_ID, info.getBatchId());
        Optionals.put(map, StageConstants.VAR_FLOW_ID, info.getFlowId());
        Optionals.put(map, StageConstants.VAR_EXECUTION_ID, info.getExecutionId());
        return VARIABLES.compareAndSet(null, map);
    }

    static void deactivate() {
        VARIABLES.set(null);
    }
}
