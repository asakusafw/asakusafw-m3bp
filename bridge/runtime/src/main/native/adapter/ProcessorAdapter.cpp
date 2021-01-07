/*
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
#include "mirror.hpp"
#include "adapter.hpp"
#include "env.hpp"
#include "jniutil.hpp"

namespace asakusafw {
namespace jni {

ProcessorAdapter::ProcessorAdapter(VertexMirror* vertex) :
        m3bp::ProcessorBase(vertex->input_ports(), vertex->output_ports()),
        m_mirror(vertex) {
}

void ProcessorAdapter::global_initialize(m3bp::Task& task) {
    LocalFrame frame(64);
    auto* env = frame.env();
    auto* vertex = m_mirror;
    auto* engine = vertex->engine();
    engine->do_global_initialize(env, vertex, task);

    jint tasks = engine->do_task_count(env, vertex);
    if (tasks >= 0) {
        task_count(static_cast<m3bp::size_type>(tasks));
    }
    jint parallels = engine->do_max_concurrency(env, vertex);
    if (parallels >= 0) {
        max_concurrency(static_cast<m3bp::size_type>(parallels));
    }
}

void ProcessorAdapter::global_finalize(m3bp::Task& task) {
    LocalFrame frame(64);
    auto* env = frame.env();
    auto* vertex = m_mirror;
    auto* engine = vertex->engine();
    engine->do_global_finalize(env, vertex, task);
}

void ProcessorAdapter::thread_local_initialize(m3bp::Task& task) {
    LocalFrame frame(64);
    auto* env = frame.env();
    auto* vertex = m_mirror;
    auto* engine = vertex->engine();
    engine->do_thread_local_initialize(env, vertex, task);
}

void ProcessorAdapter::thread_local_finalize(m3bp::Task& task) {
    LocalFrame frame(64);
    auto* env = frame.env();
    auto* vertex = m_mirror;
    auto* engine = vertex->engine();
    engine->do_thread_local_finalize(env, vertex, task);
}

void ProcessorAdapter::run(m3bp::Task& task) {
    LocalFrame frame(64);
    auto* env = frame.env();
    auto* vertex = m_mirror;
    auto* engine = vertex->engine();
    engine->do_run(env, vertex, task);
}

}  // namespace jni
}  // namespace asakusafw
