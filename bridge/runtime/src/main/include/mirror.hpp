/*
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
#ifndef MIRROR_HPP
#define MIRROR_HPP

#include <jni.h>
#include <tuple>
#include <string>
#include <memory>
#include <m3bp/m3bp.hpp>
#include <dlfcn.h>

class EngineMirror;
class ConfigurationMirror;
class FlowGraphMirror;
class VertexMirror;
class InputPortMirror;
class OutputPortMirror;

class ConfigurationMirror {
private:
    EngineMirror *m_engine;
    m3bp::Configuration m_entity;

public:
    using ValueComparatorType = std::function<bool(const void *, const void *)>;
    ConfigurationMirror(EngineMirror *engine) :
        m_engine(engine) {}
    ~ConfigurationMirror() = default;
    m3bp::Configuration &entity() {
        return m_entity;
    }
};

class FlowGraphMirror {
private:
    EngineMirror *m_engine;
    m3bp::FlowGraph m_entity;
    bool m_resolved;
    std::vector<VertexMirror*> m_children;

public:
    FlowGraphMirror(EngineMirror *engine);
    ~FlowGraphMirror();
    VertexMirror *vertex(const std::string &name);
    void edge(OutputPortMirror *upstream, InputPortMirror *downstream);
    m3bp::FlowGraph &resolve();
    m3bp::FlowGraph &entity() {
        return m_entity;
    }
};

class EngineMirror {
private:
    jobject m_mirror;
    ConfigurationMirror *m_configuration;
    FlowGraphMirror *m_graph;
    std::string m_library_name;
    void *m_library;
    jmethodID m_global_initialize_id;
    jmethodID m_global_finalize_id;
    jmethodID m_local_initialize_id;
    jmethodID m_local_finalize_id;
    jmethodID m_run_id;
    jmethodID m_task_count_id;
    jmethodID m_max_concurrency_id;
    void do_invoke(JNIEnv *env, VertexMirror *vertex, m3bp::Task &task, jmethodID method);
    jint do_invoke(JNIEnv *env, VertexMirror *vertex, jmethodID method);

public:
    EngineMirror(
        jobject object, const std::string &library_name,
        jmethodID global_initialize_id, jmethodID global_finalize_id,
        jmethodID local_initialize_id, jmethodID local_finalize_id,
        jmethodID run_id,
        jmethodID task_count_id, jmethodID max_concurrency_id);
    ~EngineMirror();
    ConfigurationMirror *configuration() {
        return m_configuration;
    }
    FlowGraphMirror *graph() {
        return m_graph;
    }
    void run();

    using ValueComparatorType = std::function<bool(const void *, const void *)>;
    ValueComparatorType load_comparator(const std::string &name);
    jobject mirror() {
        return m_mirror;
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
    std::vector<InputPortMirror*> m_inputs;
    std::vector<OutputPortMirror*> m_outputs;

public:
    VertexMirror(EngineMirror *engine, FlowGraphMirror *parent, const std::string &name);
    ~VertexMirror();
    EngineMirror *engine() {
        return m_engine;
    }
    m3bp::VertexDescriptor &resolve();
    InputPortMirror *input(
        m3bp::identifier_type, const std::string &name,
        m3bp::Movement movement, const std::string &comparator);
    OutputPortMirror *output(
        m3bp::identifier_type, const std::string &name,
        bool has_key);
    InputPortMirror *input(m3bp::identifier_type id) {
        return m_inputs[id];
    }
    OutputPortMirror *output(m3bp::identifier_type id) {
        return m_outputs[id];
    }
    std::vector<m3bp::InputPort> input_ports();
    std::vector<m3bp::OutputPort> output_ports();
};

class InputPortMirror {
private:
    EngineMirror *m_engine;
    VertexMirror *m_parent;
    m3bp::identifier_type m_id;
    m3bp::InputPort m_entity;

public:
    InputPortMirror(
        EngineMirror *engine, VertexMirror *m_parent,
        m3bp::identifier_type id, const std::string &name);
    ~InputPortMirror();
    VertexMirror *parent() {
        return m_parent;
    }
    m3bp::identifier_type id() const {
        return m_id;
    }
    m3bp::InputPort &entity() {
        return m_entity;
    }
};

class OutputPortMirror {
private:
    EngineMirror *m_engine;
    VertexMirror *m_parent;
    m3bp::identifier_type m_id;
    m3bp::OutputPort m_entity;

public:
    OutputPortMirror(
        EngineMirror *engine, VertexMirror *m_parent,
        m3bp::identifier_type id, const std::string &name);
    ~OutputPortMirror();
    VertexMirror *parent() {
        return m_parent;
    }
    m3bp::identifier_type id() const {
        return m_id;
    }
    m3bp::OutputPort &entity() {
        return m_entity;
    }
};

class InputReaderMirror {
private:
    m3bp::InputReader m_entity;
    InputPortMirror *m_port;
    bool m_has_key;
    m3bp::InputBuffer m_buffer;
    m3bp::size_type m_next_key_count;
    m3bp::size_type m_next_value_count;
    m3bp::size_type m_key_base_offset;
    m3bp::size_type m_value_base_offset;
    std::tuple<const void *, m3bp::size_type> m_key_contents;
    std::tuple<const void *, m3bp::size_type> m_key_offsets;
    std::tuple<const void *, m3bp::size_type> m_value_contents;
    std::tuple<const void *, m3bp::size_type> m_value_offsets;

public:
    InputReaderMirror(m3bp::Task *task, m3bp::identifier_type id, InputPortMirror *port);
    ~InputReaderMirror();
    bool has_key() {
        return m_has_key;
    }
    std::tuple<const void *, const void *, m3bp::size_type> key_buffer() {
        return std::make_tuple(m_buffer.key_buffer(), m_buffer.key_offset_table(), m_buffer.record_count());
    }
    std::tuple<const void *, const void *, m3bp::size_type> value_buffer() {
        return std::make_tuple(m_buffer.value_buffer(), m_buffer.value_offset_table(), m_buffer.record_count());
    }
    bool advance_key_buffer();
    bool advance_value_buffer();
    m3bp::size_type key_base_offset() {
        return m_key_base_offset;
    }
    std::tuple<const void *, m3bp::size_type> key_contents() {
        return m_key_contents;
    }
    std::tuple<const void *, m3bp::size_type> key_offsets() {
        return m_key_offsets;
    }
    m3bp::size_type value_base_offset() {
        return m_value_base_offset;
    }
    std::tuple<const void *, m3bp::size_type> value_contents() {
        return m_value_contents;
    }
    std::tuple<const void *, m3bp::size_type> value_offsets() {
        return m_value_offsets;
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
    std::tuple<const void *, m3bp::size_type> m_contents;
    std::tuple<const void *, m3bp::size_type> m_offsets;
    std::tuple<const void *, m3bp::size_type> m_key_lengths;
    void ensure();

public:
    OutputWriterMirror(m3bp::Task *task, m3bp::identifier_type id, OutputPortMirror *port);
    ~OutputWriterMirror();
    bool has_key() {
        return m_has_key;
    }
    void flush(size_t record_count);
    m3bp::size_type base_offset();
    std::tuple<const void *, m3bp::size_type, const void *, const void *, m3bp::size_type> output_buffer();
    std::tuple<const void *, size_t> contents();
    std::tuple<const void *, size_t> offsets();
    std::tuple<const void *, size_t> key_lengths();
};

class TaskMirror {
private:
    m3bp::Task *m_entity;
    VertexMirror *m_vertex;

public:
    TaskMirror(m3bp::Task &task, VertexMirror *vertex);
    ~TaskMirror();
    InputReaderMirror *input(m3bp::identifier_type id);
    OutputWriterMirror *output(m3bp::identifier_type id);
    m3bp::identifier_type logical_task_id() {
        return m_entity->logical_task_id();
    }
    m3bp::identifier_type physical_task_id() {
        return m_entity->physical_task_id();
    }
    bool is_cancelled() {
        return m_entity->is_cancelled();
    }
};

#endif // MIRROR_HPP
