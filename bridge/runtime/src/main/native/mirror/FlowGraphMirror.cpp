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
#include "util.hpp"

namespace asakusafw {
namespace jni {

FlowGraphMirror::FlowGraphMirror(EngineMirror *engine) :
        m_engine(engine),
        m_entity(m3bp::FlowGraph()),
        m_resolved(false) {
}

m3bp::FlowGraph &FlowGraphMirror::resolve() {
    if (!m_resolved) {
        for (auto& e : m_children) {
            e->resolve();
        }
    }
    return m_entity;
}

VertexMirror *FlowGraphMirror::vertex(std::string const& name) {
    auto v = make_unique<VertexMirror>(m_engine, this, name);
    auto r = v.get();
    m_children.emplace_back(std::move(v));
    return r;
}

void FlowGraphMirror::edge(OutputPortMirror *upstream, InputPortMirror *downstream) {
    auto up = upstream->parent()->resolve().output_port(upstream->id());
    auto down = downstream->parent()->resolve().input_port(downstream->id());
    m_entity.add_edge(up, down);
}

}  // namespace jni
}  // namespace asakusafw
