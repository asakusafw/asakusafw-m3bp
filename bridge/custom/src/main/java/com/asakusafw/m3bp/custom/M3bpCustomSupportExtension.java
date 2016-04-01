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
package com.asakusafw.m3bp.custom;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.bridge.stage.StageInfo;
import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.api.processor.ProcessorContext.Editor;
import com.asakusafw.dag.api.processor.extension.ProcessorContextExtension;
import com.asakusafw.dag.utils.common.InterruptibleIo;
import com.asakusafw.dag.utils.common.InterruptibleIo.Closer;
import com.asakusafw.dag.utils.common.InterruptibleIo.Initializer;

/**
 * Enables custom implementations of Asakusa Framework APIs.
 */
public class M3bpCustomSupportExtension implements ProcessorContextExtension {

    static final Logger LOG = LoggerFactory.getLogger(M3bpCustomSupportExtension.class);

    @Override
    public InterruptibleIo install(ProcessorContext context, Editor editor) throws IOException, InterruptedException {
        try (Initializer<Closer> initializer = new Initializer<>(new Closer())) {
            register(initializeBatchContext(context), initializer.get());
            return initializer.done();
        }
    }

    private void register(InterruptibleIo operation, Closer closer) {
        if (operation != null) {
            closer.add(operation);
        }
    }

    private InterruptibleIo initializeBatchContext(ProcessorContext context) {
        Optional<StageInfo> info = context.getResource(StageInfo.class);
        if (info.isPresent()) {
            LOG.debug("activating custom BatchContext");
            if (M3bpBatchContext.activate(info.get())) {
                return () -> M3bpBatchContext.deactivate();
            }
        }
        return null;
    }
}
