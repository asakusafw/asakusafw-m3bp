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
#include "mirror.hpp"
#include "adapter.hpp"

VertexMirror::VertexMirror(
        EngineMirror *engine,
        FlowGraphMirror *parent,
        const std::string &name) :
        m_engine(engine),
        m_parent(parent),
        m_name(name),
        m_resolved(false) {
}

VertexMirror::~VertexMirror() {
    for (InputPortMirror *e : m_inputs) {
        delete e;
    }
    m_inputs.clear();
    for (OutputPortMirror *e : m_outputs) {
        delete e;
    }
    m_outputs.clear();
}

m3bp::VertexDescriptor &VertexMirror::resolve() {
    if (!m_resolved) {
        m_entity = m_parent->entity().add_vertex(m_name, ProcessorAdapter(this));
        m_resolved = true;
    }
    return m_entity;
}

InputPortMirror *VertexMirror::input(
        m3bp::identifier_type id, const std::string &name,
        m3bp::Movement movement, const std::string &comparator_name) {
    auto port = new InputPortMirror(m_engine, this, id, name);
    m_inputs.insert(m_inputs.begin() + id, port);
    port->entity().movement(movement);
    auto comparator = m_engine->load_comparator(comparator_name);
    if (comparator) {
        port->entity().value_comparator(comparator);
    }
    return port;
}

OutputPortMirror *VertexMirror::output(
        m3bp::identifier_type id, const std::string &name,
        bool has_key) {
    auto port = new OutputPortMirror(m_engine, this, id, name);
    m_outputs.insert(m_outputs.begin() + id, port);
    port->entity().has_key(has_key);
    return port;
}

std::vector<m3bp::InputPort> VertexMirror::input_ports() {
    std::vector<m3bp::InputPort> results;
    for (InputPortMirror *e : m_inputs) {
        results.push_back(e->entity());
    }
    return results;
}

std::vector<m3bp::OutputPort> VertexMirror::output_ports() {
    std::vector<m3bp::OutputPort> results;
    for (OutputPortMirror *e : m_outputs) {
        results.push_back(e->entity());
    }
    return results;
}
