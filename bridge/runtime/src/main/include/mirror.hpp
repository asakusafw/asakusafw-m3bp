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
#ifndef MIRROR_HPP
#define MIRROR_HPP

#include <jni.h>
#include <tuple>
#include <string>
#include <memory>
#include <m3bp/m3bp.hpp>

namespace asakusafw {
namespace jni {

class EngineMirror;
class ConfigurationMirror;
class FlowGraphMirror;
class VertexMirror;
class InputPortMirror;
class OutputPortMirror;

class ConfigurationMirror {
private:
    m3bp::Configuration m_entity;

public:
    using ValueComparatorType = std::function<bool(void const*, void const*)>;
    ConfigurationMirror(EngineMirror *) noexcept {}
    m3bp::Configuration& entity() noexcept {
        return m_entity;
    }
};

class FlowGraphMirror {
private:
    EngineMirror *m_engine;
    m3bp::FlowGraph m_entity;
    bool m_resolved;
    std::vector<std::unique_ptr<VertexMirror>> m_children;

public:
    FlowGraphMirror(EngineMirror *engine);
    VertexMirror *vertex(std::string const& name);
    void edge(OutputPortMirror *upstream, InputPortMirror *downstream);
    m3bp::FlowGraph& resolve();
    m3bp::FlowGraph& entity() noexcept {
        return m_entity;
    }
};

class EngineMirror {
private:
    jobject m_mirror;
    std::unique_ptr<ConfigurationMirror> m_configuration;
    std::unique_ptr<FlowGraphMirror> m_graph;
    std::string m_library_name;
    void *m_library;
    std::vector<jobject> m_global_refs;
    jmethodID m_thread_initialize_id;
    jmethodID m_thread_finalize_id;
    jmethodID m_global_initialize_id;
    jmethodID m_global_finalize_id;
    jmethodID m_local_initialize_id;
    jmethodID m_local_finalize_id;
    jmethodID m_run_id;
    jmethodID m_task_count_id;
    jmethodID m_max_concurrency_id;
    void do_invoke(JNIEnv *env, jmethodID method);
    void do_invoke(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task, jmethodID method);
    jint do_invoke(JNIEnv *env, VertexMirror *vertex, jmethodID method);

public:
    EngineMirror(
        jobject object, std::string const& library_name,
        jmethodID thread_initialize_id, jmethodID thread_finalize_id,
        jmethodID global_initialize_id, jmethodID global_finalize_id,
        jmethodID local_initialize_id, jmethodID local_finalize_id,
        jmethodID run_id,
        jmethodID task_count_id, jmethodID max_concurrency_id);
    ~EngineMirror();
    ConfigurationMirror *configuration() noexcept {
        return m_configuration.get();
    }
    FlowGraphMirror *graph() noexcept {
        return m_graph.get();
    }
    void run();
    void cleanup(JNIEnv *env);

    using ValueComparatorType = std::function<bool(void const*, void const*)>;
    ValueComparatorType load_comparator(std::string const& name);
    jobject mirror() noexcept {
        return m_mirror;
    }
    void do_thread_initialize(JNIEnv *env) {
        do_invoke(env, m_thread_initialize_id);
    }
    void do_thread_finalize(JNIEnv *env) {
        do_invoke(env, m_thread_finalize_id);
    }
    void do_global_initialize(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task) {
        do_invoke(env, vertex, task, m_global_initialize_id);
    }
    void do_global_finalize(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task) {
        do_invoke(env, vertex, task, m_global_finalize_id);
    }
    void do_thread_local_initialize(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task) {
        do_invoke(env, vertex, task, m_local_initialize_id);
    }
    void do_thread_local_finalize(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task) {
        do_invoke(env, vertex, task, m_local_finalize_id);
    }
    void do_run(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task) {
        do_invoke(env, vertex, task, m_run_id);
    }
    jint do_task_count(JNIEnv *env, VertexMirror *vertex) {
        return do_invoke(env, vertex, m_task_count_id);
    }
    jint do_max_concurrency(JNIEnv *env, VertexMirror *vertex) {
        return do_invoke(env, vertex, m_max_concurrency_id);
    }
};

class VertexMirror {
private:
    EngineMirror *m_engine;
    FlowGraphMirror *m_parent;
    std::string m_name;
    m3bp::VertexDescriptor m_entity;
    bool m_resolved;
    std::vector<std::unique_ptr<InputPortMirror>> m_inputs;
    std::vector<std::unique_ptr<OutputPortMirror>> m_outputs;

public:
    VertexMirror(EngineMirror *engine, FlowGraphMirror *parent, std::string const& name);
    EngineMirror *engine() noexcept {
        return m_engine;
    }
    m3bp::VertexDescriptor &resolve();
    InputPortMirror* input(
        m3bp::identifier_type, std::string const& name,
        m3bp::Movement movement, std::string const& comparator);
    OutputPortMirror* output(
        m3bp::identifier_type, std::string const& name,
        bool has_key);
    InputPortMirror* input(m3bp::identifier_type id) {
        return m_inputs[id].get();
    }
    OutputPortMirror* output(m3bp::identifier_type id) {
        return m_outputs[id].get();
    }
    std::vector<m3bp::InputPort> input_ports();
    std::vector<m3bp::OutputPort> output_ports();
};

class InputPortMirror {
private:
    VertexMirror *m_parent;
    m3bp::identifier_type m_id;
    m3bp::InputPort m_entity;

public:
    InputPortMirror(
        EngineMirror *engine, VertexMirror *m_parent,
        m3bp::identifier_type id, std::string const& name);
    VertexMirror *parent() {
        return m_parent;
    }
    m3bp::identifier_type id() const noexcept {
        return m_id;
    }
    m3bp::InputPort &entity() noexcept {
        return m_entity;
    }
};

class OutputPortMirror {
private:
    VertexMirror *m_parent;
    m3bp::identifier_type m_id;
    m3bp::OutputPort m_entity;

public:
    OutputPortMirror(
        EngineMirror *engine, VertexMirror *m_parent,
        m3bp::identifier_type id, std::string const&name);
    VertexMirror *parent() const noexcept {
        return m_parent;
    }
    m3bp::identifier_type id() const noexcept {
        return m_id;
    }
    m3bp::OutputPort &entity() noexcept {
        return m_entity;
    }
};

class InputReaderMirror {
private:
    m3bp::InputReader m_entity;
    bool m_has_key;
    m3bp::InputBuffer m_buffer;

public:
    InputReaderMirror(m3bp::Task *task, m3bp::identifier_type id, InputPortMirror *port);
    ~InputReaderMirror();
    bool has_key() {
        return m_has_key;
    }
    std::tuple<void const*, void const*, m3bp::size_type> key_buffer() const noexcept {
        return std::make_tuple(m_buffer.key_buffer(), m_buffer.key_offset_table(), m_buffer.record_count());
    }
    std::tuple<void const*, void const*, m3bp::size_type> value_buffer() const noexcept {
        return std::make_tuple(m_buffer.value_buffer(), m_buffer.value_offset_table(), m_buffer.record_count());
    }
};

class OutputWriterMirror {
private:
    m3bp::OutputWriter m_entity;
    OutputPortMirror *m_port;
    bool m_has_key;
    m3bp::OutputBuffer m_buffer;
    bool m_ensured;
    m3bp::size_type m_base_offset;
    std::tuple<void const*, m3bp::size_type> m_contents;
    std::tuple<void const*, m3bp::size_type> m_offsets;
    std::tuple<void const*, m3bp::size_type> m_key_lengths;
    void ensure();

public:
    OutputWriterMirror(m3bp::Task *task, m3bp::identifier_type id, OutputPortMirror *port);
    ~OutputWriterMirror();
    bool has_key() {
        return m_has_key;
    }
    void flush(std::size_t record_count);
    m3bp::size_type base_offset();
    std::tuple<void const*, m3bp::size_type, void const*, void const*, m3bp::size_type> output_buffer();
    std::tuple<void const*, std::size_t> contents();
    std::tuple<void const*, std::size_t> offsets();
    std::tuple<void const*, std::size_t> key_lengths();
};

class TaskMirror {
private:
    m3bp::Task *m_entity;
    VertexMirror *m_vertex;

public:
    TaskMirror(m3bp::Task &task, VertexMirror *vertex);
    ~TaskMirror();
    InputReaderMirror* input(m3bp::identifier_type id);
    OutputWriterMirror* output(m3bp::identifier_type id);
    m3bp::identifier_type logical_task_id() const noexcept {
        return m_entity->logical_task_id();
    }
    m3bp::identifier_type physical_task_id() const noexcept {
        return m_entity->physical_task_id();
    }
    bool is_cancelled() const noexcept {
        return m_entity->is_cancelled();
    }
};

}  // namespace jni
}  // namespace asakusafw

#endif // MIRROR_HPP
