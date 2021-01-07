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
#ifndef ADAPTER_HPP
#define ADAPTER_HPP

#include <m3bp/m3bp.hpp>

#include "jniutil.hpp"

namespace asakusafw {
namespace jni {

class VertexMirror;
class EngineMirror;

class ProcessorAdapter : public m3bp::ProcessorBase {
private:
    VertexMirror *m_mirror;

public:
    ProcessorAdapter(VertexMirror *mirror);
    void global_initialize(m3bp::Task& task) override;
    void global_finalize(m3bp::Task& task) override;
    void thread_local_initialize(m3bp::Task& task) override;
    void thread_local_finalize(m3bp::Task& task) override;
    void run(m3bp::Task& task) override;
};

class ThreadObserverAdapter : public m3bp::ThreadObserverBase {
private:
    EngineMirror *m_engine;

public:
    ThreadObserverAdapter(EngineMirror *engine) :
            m3bp::ThreadObserverBase(),
            m_engine(engine) {}
    void on_initialize() override;
    void on_finalize() override;
};

}  // namespace jni
}  // namespace asakusafw

#endif // ADAPTER_HPP
