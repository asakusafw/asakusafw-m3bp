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
package com.asakusafw.m3bp.workaround.hadoop;

import java.io.IOException;

import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.processor.ProcessorContext;
import com.asakusafw.dag.api.processor.ProcessorContext.Editor;
import com.asakusafw.dag.api.processor.extension.ProcessorContextExtension;
import com.asakusafw.dag.utils.common.InterruptibleIo;

/**
 * Forcibly wake-up {@link WakeUpUserGroupInformation}.
 * @since 0.1.1
 */
public class WakeUpUserGroupInformation implements ProcessorContextExtension {

    static final Logger LOG = LoggerFactory.getLogger(WakeUpUserGroupInformation.class);

    @Override
    public InterruptibleIo install(ProcessorContext context, Editor editor) throws IOException, InterruptedException {
        LOG.info("workaround: eager wake up UserGroupInformation");
        UserGroupInformation.isSecurityEnabled();
        return null;
    }
}
