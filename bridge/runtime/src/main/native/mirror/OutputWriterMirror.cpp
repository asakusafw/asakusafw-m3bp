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
#include <algorithm>
#include <stdexcept>
#include <cstdint>

// NOTE: offset buffer requires "(#-of-entries + 1) * sizeof(m3bp::size_type)" bytes
static const m3bp::size_type MAX_ENTRIES = INT32_MAX / sizeof(m3bp::size_type);

static void put(std::tuple<const void *, m3bp::size_type> &t, const void *begin, m3bp::size_type length) {
    std::get<0>(t) = begin;
    std::get<1>(t) = length;
}

OutputWriterMirror::OutputWriterMirror(m3bp::Task *task, m3bp::identifier_type id, OutputPortMirror *port) :
        m_entity(task->output(id)),
        m_port(port),
        m_buffer_size(0),
        m_record_count(0),
        m_has_key(m_port->entity().has_key()),
        m_ensured(false) {
}

OutputWriterMirror::~OutputWriterMirror() = default;

void OutputWriterMirror::ensure() {
    if (!m_ensured) {
        allocate();

        auto contents = m_buffer.data_buffer();
        auto contents_size = m_buffer.data_buffer_size();
        put(m_contents, contents, contents_size);
        m_base_offset = 0; // Note: base offset is always 0 in this implementation

        auto entries = m_buffer.offset_table();
        auto entries_size = (std::min(m_buffer.max_record_count(), MAX_ENTRIES) + 1) * sizeof(m3bp::size_type);
        put(m_offsets, entries, entries_size);

        if (m_has_key) {
            auto keys = m_buffer.key_length_table();
            auto keys_size = entries_size - sizeof(m3bp::size_type);
            put(m_key_lengths, keys, keys_size);
        } else {
            put(m_key_lengths, nullptr, 0);
        }
        m_ensured = true;
    }
}

std::tuple<const void *, m3bp::size_type, const void *, const void *, m3bp::size_type> OutputWriterMirror::output_buffer() {
    if (!m_ensured) {
        allocate();
        m_ensured = true;
    }
    return std::make_tuple(
            m_buffer.data_buffer(), m_buffer.data_buffer_size(),
            m_buffer.offset_table(),
            m_has_key ? m_buffer.key_length_table() : nullptr,
            m_buffer.max_record_count());
}

void OutputWriterMirror::allocate() {
    if (!m_ensured) {
        if (m_buffer_size > 0 && m_record_count > 0) {
            m_buffer = m_entity.allocate_buffer(m_buffer_size, m_record_count);
        } else {
            m_buffer = m_entity.allocate_buffer();
        }
    }
}

m3bp::size_type OutputWriterMirror::base_offset() {
    ensure();
    return m_base_offset;
}

std::tuple<const void *, size_t> OutputWriterMirror::contents() {
    ensure();
    return m_contents;
}
std::tuple<const void *, size_t> OutputWriterMirror::offsets() {
    ensure();
    return m_offsets;
}
std::tuple<const void *, size_t> OutputWriterMirror::key_lengths() {
    ensure();
    return m_key_lengths;
}

void OutputWriterMirror::flush(m3bp::size_type record_count) {
    m_entity.flush_buffer(std::move(m_buffer), record_count);
    m_ensured = false;
}
