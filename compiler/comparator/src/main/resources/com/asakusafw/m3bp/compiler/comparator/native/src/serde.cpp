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
#include "serde.hpp"
#include "mpdecimal.hpp"

namespace asakusafw {
namespace serde {

int compare_decimal(std::int8_t *&a, std::int8_t *&b) {
    auto head_a = read_value<std::int8_t>(a);
    auto head_b = read_value<std::int8_t>(b);
    if (head_a == DECIMAL_NULL) {
        if (head_b == DECIMAL_NULL) {
            return 0;
        }
        return -1;
    }
    if (head_b == DECIMAL_NULL) {
        return +1;
    }
    bool plus_a = (head_a & DECIMAL_PLUS_MASK) != 0;
    bool plus_b = (head_b & DECIMAL_PLUS_MASK) != 0;
    if (plus_a != plus_b) {
        return plus_a ? +1 : -1;
    }
    bool compact_a = (head_a & DECIMAL_COMPACT_MASK) != 0;
    bool compact_b = (head_b & DECIMAL_COMPACT_MASK) != 0;
    auto scale_a = static_cast<std::int32_t>(read_compact_int(a));
    auto scale_b = static_cast<std::int32_t>(read_compact_int(b));
    std::int64_t unscaled_a = read_compact_int(a);
    std::int64_t unscaled_b = read_compact_int(b);
    assert(unscaled_a >= 0);
    assert(unscaled_b >= 0);
    asakusafw::math::Sign sign;
    if (compact_a && compact_b) {
        sign = asakusafw::math::compare_decimal(
                static_cast<std::uint64_t>(unscaled_a), -scale_a,
                static_cast<std::uint64_t>(unscaled_b), -scale_b);
    } else if (compact_a) {
        auto* b_buf = reinterpret_cast<std::uint8_t*>(b);
        auto b_length = static_cast<std::size_t>(unscaled_b);
        b += b_length;
        sign = asakusafw::math::compare_decimal(
                static_cast<std::uint64_t>(unscaled_a), -scale_a,
                b_buf, b_length, -scale_b);
    } else if (compact_b) {
        auto* a_buf = reinterpret_cast<std::uint8_t*>(a);
        auto a_length = static_cast<std::size_t>(unscaled_a);
        a += a_length;
        sign = asakusafw::math::compare_decimal(
                a_buf, a_length, -scale_a,
                static_cast<std::uint64_t>(unscaled_b), -scale_b);
    } else {
        auto* a_buf = reinterpret_cast<std::uint8_t*>(a);
        auto a_length = static_cast<std::size_t>(unscaled_a);
        a += a_length;
        auto* b_buf = reinterpret_cast<std::uint8_t*>(b);
        auto b_length = static_cast<std::size_t>(unscaled_b);
        b += b_length;
        sign = asakusafw::math::compare_decimal(
                a_buf, a_length, -scale_a,
                b_buf, b_length, -scale_b);
    }
    return static_cast<int>(plus_a ? sign : asakusafw::math::negate(sign));
}

} // namespace serde
} // namespace asakusafw
