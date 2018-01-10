/*
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
#include "mirror.hpp"
#include "jniutil.hpp"
#include "adapter.hpp"
#include <memory>
#include <stdexcept>
#include <dlfcn.h>

EngineMirror::EngineMirror(
        jobject mirror, const std::string &library_name,
        jmethodID thread_initialize_id, jmethodID thread_finalize_id,
        jmethodID global_initialize_id, jmethodID global_finalize_id,
        jmethodID local_initialize_id, jmethodID local_finalize_id,
        jmethodID run_id,
        jmethodID task_count_id, jmethodID max_concurrency_id) :
        m_mirror(mirror),
        m_library_name(library_name),
        m_library(0),
        m_thread_initialize_id(thread_initialize_id), m_thread_finalize_id(thread_finalize_id),
        m_global_initialize_id(global_initialize_id), m_global_finalize_id(global_finalize_id),
        m_local_initialize_id(local_initialize_id), m_local_finalize_id(local_finalize_id),
        m_run_id(run_id),
        m_task_count_id(task_count_id), m_max_concurrency_id(max_concurrency_id) {
    if (!library_name.empty()) {
        m_library = dlopen(library_name.data(), RTLD_LAZY);
    }
    m_configuration = new ConfigurationMirror(this);
    m_graph = new FlowGraphMirror(this);
}

EngineMirror::~EngineMirror() {
    if (m_library) {
        dlclose(m_library);
    }
    delete m_configuration;
    delete m_graph;
}

using ValueComparatorFunc = bool(const void *, const void *);
using ValueComparatorType = std::function<ValueComparatorFunc>;
ValueComparatorType EngineMirror::load_comparator(const std::string &name) {
    if (name.empty()) {
        return nullptr;
    }
    if (!m_library) {
        throw std::runtime_error("value comparator library is not specified");
    }
    auto *func = (ValueComparatorFunc*) dlsym(m_library, name.data());
    if (!func) {
        throw std::runtime_error("unknown comparator: " + name + " (" + m_library_name + ")");
    }
    return static_cast<ValueComparatorType>(func);
}

void EngineMirror::run() {
    m3bp::Context context;
    context.set_flow_graph(m_graph->resolve());
    context.set_configuration(m_configuration->entity());
    context.add_thread_observer(std::make_shared<ThreadObserverAdapter>(this));
    context.execute();
    context.wait();
}

void EngineMirror::cleanup(JNIEnv *env) {
    for (jobject &global_ref : m_global_refs) {
        if (global_ref) {
            env->DeleteGlobalRef(global_ref);
            global_ref = nullptr;
        }
    }
    m_global_refs.clear();
}

void EngineMirror::do_invoke(JNIEnv *env, jmethodID method) {
    env->CallVoidMethod(m_mirror, method);
    check_java_exception(env, m_global_refs);
}

void EngineMirror::do_invoke(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task, jmethodID method) {
    TaskMirror mirror(task, vertex);
    env->CallVoidMethod(m_mirror, method, to_pointer(vertex), to_pointer(&mirror));
    check_java_exception(env, m_global_refs);
}

jint EngineMirror::do_invoke(JNIEnv *env, VertexMirror *vertex, jmethodID method) {
    jint result = env->CallIntMethod(m_mirror, method, to_pointer(vertex));
    check_java_exception(env, m_global_refs);
    return result;
}
