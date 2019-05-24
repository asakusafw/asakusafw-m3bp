/*
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
#include "adapter.hpp"
#include "mirror.hpp"
#include "env.hpp"

namespace asakusafw {
namespace jni {

void ThreadObserverAdapter::on_initialize() {
    auto* env = java_attach();
    m_engine->do_thread_initialize(env);
}

void ThreadObserverAdapter::on_finalize() {
    auto* env = java_env();
    if (env) {
        m_engine->do_thread_finalize(env);
    }
    java_detach();
}

}  // namespace jni
}  // namespace asakusafw
