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
#include <cstdint>
#include <cstring>

namespace asakusafw {
namespace m3bp {
namespace testing {

template<class T>
static inline bool less(void const* a, void const* b) {
    T va, vb;
    std::memcpy(&va, a, sizeof(T));
    std::memcpy(&vb, b, sizeof(T));
    return va < vb;
}

extern "C" bool lt_int32(void const* a, void const* b) {
    return less<std::int32_t>(a, b);
}

}  // namespace testing
}  // namespace m3bp
}  // namespace asakusafw
