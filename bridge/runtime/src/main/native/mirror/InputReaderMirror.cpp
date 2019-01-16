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
#include "mirror.hpp"

InputReaderMirror::InputReaderMirror(m3bp::Task *task, m3bp::identifier_type id, InputPortMirror *port) :
        m_entity(task->input(id)),
        m_has_key(port->entity().movement() == m3bp::Movement::SCATTER_GATHER),
        m_buffer(m_entity.raw_buffer()) {
}

InputReaderMirror::~InputReaderMirror() = default;
