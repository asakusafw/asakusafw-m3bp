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
#include <cstdint>
#include <stdexcept>

static const m3bp::size_type MAX_BUFFER_SIZE = INT32_MAX;

static void put(std::tuple<const void *, m3bp::size_type> &t, const void *begin, m3bp::size_type length) {
    std::get<0>(t) = begin;
    std::get<1>(t) = length;
}

static m3bp::size_type fit(
        const m3bp::size_type *table,
        m3bp::size_type start,
        m3bp::size_type count) {
    m3bp::size_type last = count;
    m3bp::size_type base = table[start];
    while (start < last) {
        m3bp::size_type chunk_size = table[last] - base;
        if (chunk_size <= MAX_BUFFER_SIZE) {
            return last - start;
        }
        last = (last - start) / 2 + start;
    }
    throw std::runtime_error("input group is too large; please use larger addressing mode instead");
}

InputReaderMirror::InputReaderMirror(m3bp::Task *task, m3bp::identifier_type id, InputPortMirror *port) :
        m_entity(task->input(id)),
        m_has_key(port->entity().movement() == m3bp::Movement::SCATTER_GATHER),
        m_buffer(m_entity.raw_buffer()) {
    put(m_key_offsets, m_buffer.key_offset_table(), 0);
    put(m_key_contents, m_buffer.key_buffer(), 0);
    put(m_value_offsets, m_buffer.value_offset_table(), 0);
    put(m_value_contents, m_buffer.value_buffer(), 0);
    m_key_base_offset = 0;
    m_value_base_offset = 0;
    m_next_key_count = 0;
    m_next_value_count= 0;
}

InputReaderMirror::~InputReaderMirror() = default;

bool InputReaderMirror::advance_key_buffer() {
    auto start = m_next_key_count;
    auto count = m_buffer.record_count();
    if (start >= count) {
        return false;
    }
    auto offsets = m_buffer.key_offset_table();
    auto length = fit(offsets, start, count);
    auto next = start + length;
    put(m_key_offsets, offsets + start, (length + 1) * sizeof(m3bp::size_type));

    auto base = offsets[start];
    auto limit = offsets[next];
    auto buffer = static_cast<const uint8_t *>(m_buffer.key_buffer());
    put(m_key_contents, buffer + base, limit - base);

    m_key_base_offset = base;
    m_next_key_count = next;
    return true;
}

bool InputReaderMirror::advance_value_buffer() {
    auto start = m_next_value_count;
    auto count = m_buffer.record_count();
    if (start >= count) {
        return false;
    }
    auto *offsets = m_buffer.value_offset_table();
    auto length = fit(offsets, start, count);
    auto next = start + length;
    put(m_value_offsets, offsets + start, (length + 1) * sizeof(m3bp::size_type));

    auto base = offsets[start];
    auto limit = offsets[next];
    auto buffer = static_cast<const uint8_t *>(m_buffer.value_buffer());
    put(m_value_contents, buffer + base, limit - base);

    m_value_base_offset = base;
    m_next_value_count = next;
    return true;
}
