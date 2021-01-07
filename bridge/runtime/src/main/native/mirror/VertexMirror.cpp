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
#include "util.hpp"

namespace asakusafw {
namespace jni {

VertexMirror::VertexMirror(
        EngineMirror *engine,
        FlowGraphMirror *parent,
        const std::string &name) :
        m_engine(engine),
        m_parent(parent),
        m_name(name),
        m_resolved(false) {
}

m3bp::VertexDescriptor &VertexMirror::resolve() {
    if (!m_resolved) {
        m_entity = m_parent->entity().add_vertex(m_name, ProcessorAdapter(this));
        m_resolved = true;
    }
    return m_entity;
}

InputPortMirror *VertexMirror::input(
        m3bp::identifier_type id, std::string const& name,
        m3bp::Movement movement, std::string const& comparator_name) {
    auto port = make_unique<InputPortMirror>(m_engine, this, id, name);
    auto* p = port.get();
    m_inputs.insert(m_inputs.begin() + id, std::move(port));
    p->entity().movement(movement);
    if (!comparator_name.empty()) {
        auto comparator = m_engine->load_comparator(comparator_name);
        p->entity().value_comparator(std::move(comparator));
    }
    return p;
}

OutputPortMirror *VertexMirror::output(
        m3bp::identifier_type id, std::string const& name,
        bool has_key) {
    auto port = make_unique<OutputPortMirror>(m_engine, this, id, name);
    auto* p = port.get();
    m_outputs.insert(m_outputs.begin() + id, std::move(port));
    p->entity().has_key(has_key);
    return p;
}

std::vector<m3bp::InputPort> VertexMirror::input_ports() {
    std::vector<m3bp::InputPort> results;
    results.reserve(m_inputs.size());
    for (auto& e : m_inputs) {
        results.emplace_back(e->entity());
    }
    return results;
}

std::vector<m3bp::OutputPort> VertexMirror::output_ports() {
    std::vector<m3bp::OutputPort> results;
    results.reserve(m_outputs.size());
    for (auto& e : m_outputs) {
        results.emplace_back(e->entity());
    }
    return results;
}

}  // namespace jni
}  // namespace asakusafw
