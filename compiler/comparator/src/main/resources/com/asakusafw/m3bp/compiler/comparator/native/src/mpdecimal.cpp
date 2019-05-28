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
#include "mpdecimal.hpp"

#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <cmath>
#include <array>
#include <vector>
#include <tuple>
#include <mutex>
#include <numeric>
#include <algorithm>

namespace asakusafw {
namespace math {

#define RETURN_IF_DIFFERENT(diff) \
{\
    Sign _difference = (diff);\
    if (_difference != ::asakusafw::math::Sign::equal_to) return _difference;\
}

inline
static std::size_t count_leading_zero_bits(std::uint32_t value) {
    if (value == 0) {
        return 32;
    }
    constexpr std::uint32_t msb = 1 << 31;
    std::size_t result = 0;
    for (std::size_t i = 0, n = sizeof(value) * 8; i < n; ++i) {
        if (value & msb) {
            break;
        }
        ++result;
        value <<= 1;
    }
    return result;
}

template<typename T>
inline static T get_uint(std::uint8_t const* bytes, std::size_t count) {
    assert(0 <= count);
    assert(count <= sizeof(T));
    T result = 0;
    for (std::size_t i = 0; i < count; ++i) {
        result = result << 8 | bytes[i];
    }
    return result;
}

inline
static void shrink_vector(std::vector<std::uint32_t>& vector) {
    while (!vector.empty() && vector.back() == 0) {
        vector.pop_back();
    }
    assert(vector.empty() || vector.back() != 0);
}

MpInt::MpInt(std::uint64_t value) {
    if (value != 0) {
        auto hi = static_cast<std::uint32_t>(value >> 32);
        if (hi == 0) {
            m_members.reserve(1);
            m_members.push_back(static_cast<std::uint32_t>(value));
        } else {
            m_members.reserve(2);
            m_members.push_back(static_cast<std::uint32_t>(value));
            m_members.push_back(hi);
        }
    }
    assert(m_members.empty() || m_members.back() != 0);
}

MpInt::MpInt(std::uint8_t const* bytes, std::size_t size_in_bytes) {
    // skip leading zeros
    std::uint8_t const* buf = bytes;
    std::size_t len = size_in_bytes;
    while (len > 0) {
        if (*buf != 0) {
            break;
        }
        buf++;
        len--;
    }
    if (len != 0) {
        std::size_t blocks = (len + (sizeof(std::uint32_t) - 1)) / sizeof(std::uint32_t);
        m_members.reserve(blocks);
        while (len >= sizeof(std::uint32_t)) {
            len -= sizeof(std::uint32_t);
            m_members.push_back(get_uint<std::uint32_t>(buf + len, sizeof(std::uint32_t)));
        }
        if (len != 0) {
            m_members.push_back(get_uint<std::uint32_t>(buf, len));
        }
    }
    assert(m_members.empty() || m_members.back() != 0);
}

MpInt::MpInt(const std::vector<std::uint32_t>& members) : m_members(members) {
    shrink_vector(m_members);
}

MpInt::MpInt(std::vector<std::uint32_t>&& members) : m_members(members) {
    shrink_vector(m_members);
}

std::size_t MpInt::bits() const {
    assert(m_members.empty() || m_members.back() != 0);
    if (m_members.empty()) {
        return 0;
    }
    std::size_t block_bits = m_members.size() * 32;
    std::size_t zero_bits = count_leading_zero_bits(m_members.back());
    assert(zero_bits < 32); // the last word must not be zero
    return block_bits - zero_bits;
}

std::vector<std::uint8_t> MpInt::data() const {
    std::size_t size = (bits() + 7) / 8;
    if (size == 0) {
        return {};
    }
    assert(!m_members.empty());
    std::vector<std::uint8_t> results;
    results.reserve(size);
    int first_size = size % sizeof(std::uint32_t);
    bool first = first_size != 0;
    for (auto itr = m_members.rbegin(); itr != m_members.rend(); ++itr) {
        std::uint32_t value = *itr;
        if (first) {
            assert(value != 0);
            for (int i = first_size - 1; i >= 0; --i) {
                results.push_back(static_cast<std::uint8_t>(value >> (i * 8)));
            }
            first = false;
        } else {
            results.push_back(static_cast<std::uint8_t>(value >> 24));
            results.push_back(static_cast<std::uint8_t>(value >> 16));
            results.push_back(static_cast<std::uint8_t>(value >>  8));
            results.push_back(static_cast<std::uint8_t>(value >>  0));
        }
    }
    assert(results.size() == size);
    assert(results.front() != 0);
    return results;
}

MpInt MpInt::operator*(std::uint32_t multiplier) const {
    if (multiplier == 0) {
        return {};
    }
    if (multiplier == 1) {
        return *this;
    }
    std::size_t current_bits = bits();
    if (current_bits == 0) {
        return {};
    }
    if (current_bits == 1) {
        return MpInt(static_cast<std::uint64_t>(multiplier));
    }
    std::size_t result_bits = current_bits + 32 - count_leading_zero_bits(multiplier);
    std::vector<std::uint32_t> results;
    results.reserve((result_bits + 31) / 32);

    std::uint64_t work = 0;
    for (auto itr = m_members.begin(); itr != m_members.end(); ++itr) {
        work += static_cast<std::uint64_t>(*itr) * multiplier;
        results.push_back(static_cast<std::uint32_t>(work));
        work >>= 32; // carry-up
    }
    if (work != 0) {
        results.push_back(static_cast<std::uint32_t>(work));
    }
    return MpInt(std::move(results));
}

MpInt MpInt::operator*(MpInt const& multiplier) const {
    if (m_members.empty() || multiplier.m_members.empty()) {
        return MpInt();
    }
    std::size_t a_size = m_members.size();
    std::size_t b_size = multiplier.m_members.size();
    if (a_size > b_size) {
        return multiplier * *this;
    }
    std::size_t current_bits = bits();
    if (current_bits == 1) {
        return multiplier;
    }
    std::size_t other_bits = multiplier.bits();
    if (other_bits == 1) {
        return *this;
    }
    std::size_t result_bits = current_bits + other_bits;
    std::vector<std::uint32_t> results((result_bits + 31) / 32, 0);
    std::size_t c_size = results.size();
    for (std::size_t i = 0; i < a_size; ++i) {
        std::uint64_t a = static_cast<std::uint64_t>(m_members[i]);
        std::uint64_t work = 0;
        for (std::size_t j = 0; j < b_size; ++j) {
            assert(work <= UINT64_C(0xffffffff));
            std::uint64_t b = static_cast<std::uint64_t>(multiplier.m_members[j]);
            int k = i + j;
            std::uint32_t &c = results[k];
            work += a * b + c;
            c = static_cast<std::uint32_t>(work);
            work >>= 32;
        }
        for (std::size_t j = b_size; j < c_size; j++) {
            if (work == 0) {
                break;
            }
            assert(work <= UINT64_C(0xffffffff));
            int k = i + j;
            std::uint32_t &c = results[k];
            work += c;
            c = static_cast<std::uint32_t>(work);
            work >>= 32;
        }
        assert(work == 0);
    }
    return MpInt(std::move(results));
}

template<typename T>
constexpr inline static Sign compare_value(T a, T b) {
    return a == b ? Sign::equal_to : a < b ? Sign::less_than : Sign::greater_than;
}

Sign MpInt::compare_to(std::uint64_t other) const {
    std::size_t words = m_members.size();
    if (words == 0) {
        return compare_value(UINT64_C(0), other);
    }
    if (words == 1) {
        RETURN_IF_DIFFERENT(compare_value(UINT32_C(0), static_cast<std::uint32_t>(other >> 32)));
        RETURN_IF_DIFFERENT(compare_value(m_members[0], static_cast<std::uint32_t>(other)));
        return Sign::equal_to;
    }
    if (words == 2) {
        RETURN_IF_DIFFERENT(compare_value(m_members[1], static_cast<std::uint32_t>(other >> 32)));
        RETURN_IF_DIFFERENT(compare_value(m_members[0], static_cast<std::uint32_t>(other)));
        return Sign::equal_to;
    }
    return Sign::greater_than;
}

Sign MpInt::compare_to(MpInt const& other) const {
    RETURN_IF_DIFFERENT(compare_value(bits(), other.bits()));
    assert(m_members.size() == other.m_members.size());
    for (auto itr_a = m_members.rbegin(), itr_b = other.m_members.rbegin();
            itr_a != m_members.rend();
            ++itr_a, ++itr_b) {
        assert(itr_b != other.m_members.rend());
        RETURN_IF_DIFFERENT(compare_value(*itr_a, *itr_b));
    }
    return Sign::equal_to;
}

static
const std::array<std::uint64_t, 20> s_compact_exponents({{
    UINT64_C(1),
    UINT64_C(10),
    UINT64_C(100),
    UINT64_C(1000),
    UINT64_C(10000),
    UINT64_C(100000),
    UINT64_C(1000000),
    UINT64_C(10000000),
    UINT64_C(100000000),
    UINT64_C(1000000000),
    UINT64_C(10000000000),
    UINT64_C(100000000000),
    UINT64_C(1000000000000),
    UINT64_C(10000000000000),
    UINT64_C(100000000000000),
    UINT64_C(1000000000000000),
    UINT64_C(10000000000000000),
    UINT64_C(100000000000000000),
    UINT64_C(1000000000000000000),
    UINT64_C(10000000000000000000), // requires just 64-bits
}});

static std::mutex s_exponents_table_mutex;
static std::vector<MpInt> s_exponents_table;

MpInt const& MpInt::power_of_10(std::uint32_t exponent) {
    // Elements of s_exponents_table must be thread-safe.
    // To prevent from re-allocating MpInt, vector element must be a pointer
    std::lock_guard<std::mutex> lock(s_exponents_table_mutex);
    if (exponent >= s_exponents_table.size()) {
        if (s_exponents_table.empty()) {
            s_exponents_table.reserve(std::max(UINT32_C(64), exponent + 1));
            for (auto compact : s_compact_exponents) {
                s_exponents_table.emplace_back(compact);
            }
        }
        assert(s_exponents_table.size() >= 1);
        for (std::size_t i = s_exponents_table.size(); i <= exponent; i++) {
            auto& last = s_exponents_table.back();
            s_exponents_table.emplace_back(last * 10);
        }
    }
    assert(exponent < s_exponents_table.size());
    return s_exponents_table[exponent];
}

static
Sign compare_with_exponent(std::uint64_t a, std::uint64_t b, std::uint32_t exponent);

static
Sign compare_with_exponent(MpInt const& a, std::uint64_t b, std::uint32_t exponent);

static
Sign compare_with_exponent(std::uint64_t a, MpInt const& b, std::uint32_t exponent);

static
Sign compare_with_exponent(MpInt const& a, MpInt const& b, std::uint32_t exponent);

Sign CompactDecimal::compare_to(CompactDecimal const& other) const {
    std::uint64_t a = significand();
    std::uint64_t b = other.significand();
    std::int32_t a_exponent = exponent();
    std::int32_t b_exponent = other.exponent();
    if (a_exponent == b_exponent) {
        return compare_value(a, b);
    }
    if (a_exponent < b_exponent) {
        auto exponent = static_cast<std::uint32_t>(b_exponent - a_exponent);
        return compare_with_exponent(a, b, exponent);
    }
    assert(a_exponent > b_exponent);
    auto exponent = static_cast<std::uint32_t>(a_exponent - b_exponent);
    return negate(compare_with_exponent(b, a, exponent));
}

Sign CompactDecimal::compare_to(MpDecimal const& other) const {
    return negate(other.compare_to(*this));
}

Sign MpDecimal::compare_to(CompactDecimal const& other) const {
    MpInt const& a = significand();
    std::uint64_t b = other.significand();
    std::int32_t a_exponent = exponent();
    std::int32_t b_exponent = other.exponent();
    if (a_exponent == b_exponent) {
        return significand().compare_to(other.significand());
    }
    if (a_exponent < b_exponent) {
        auto exponent = static_cast<std::uint32_t>(b_exponent - a_exponent);
        return compare_with_exponent(a, b, exponent);
    }
    assert(a_exponent > b_exponent);
    auto exponent = static_cast<std::uint32_t>(a_exponent - b_exponent);
    return negate(compare_with_exponent(b, a, exponent));
}

Sign MpDecimal::compare_to(MpDecimal const& other) const {
    MpInt const& a = significand();
    MpInt const& b = other.significand();
    std::int32_t a_exponent = exponent();
    std::int32_t b_exponent = other.exponent();
    if (a_exponent == b_exponent) {
        return significand().compare_to(other.significand());
    }
    if (a_exponent < b_exponent) {
        auto exponent = static_cast<std::uint32_t>(b_exponent - a_exponent);
        return compare_with_exponent(a, b, exponent);
    }
    assert(a_exponent > b_exponent);
    auto exponent = static_cast<std::uint32_t>(a_exponent - b_exponent);
    return negate(compare_with_exponent(b, a, exponent));
}

Sign compare_decimal(
        std::uint8_t const* a_buf, std::size_t a_length, std::int32_t a_exponent,
        std::uint8_t const* b_buf, std::size_t b_length, std::int32_t b_exponent) {
    MpDecimal a(a_buf, a_length, a_exponent);
    MpDecimal b(b_buf, b_length, b_exponent);
    return a.compare_to(b);
}

Sign compare_decimal(
        std::uint8_t const* a_buf, std::size_t a_length, std::int32_t a_exponent,
        std::uint64_t b_significand, std::int32_t b_exponent) {
    MpDecimal a(a_buf, a_length, a_exponent);
    CompactDecimal b(b_significand, b_exponent);
    return a.compare_to(b);
}

Sign compare_decimal(
        std::uint64_t a_significand, std::int32_t a_exponent,
        std::uint64_t b_significand, std::int32_t b_exponent) {
    CompactDecimal a(a_significand, a_exponent);
    CompactDecimal b(b_significand, b_exponent);
    return a.compare_to(b);
}

static
Sign compare_with_exponent(std::uint64_t a, std::uint64_t b, std::uint32_t exponent) {
    if (a == UINT64_C(0) || b == UINT64_C(0)) {
        return compare_value(a, b);
    }
    if (exponent < s_compact_exponents.size()) {
        // (a <=> b*10^{exponent}) == (a / 10^{exponent} <=> b)
        std::uint64_t s = s_compact_exponents[exponent];
        std::uint64_t div = a / s;
        std::uint64_t mod = a % s;
        RETURN_IF_DIFFERENT(compare_value(div, b));
        return compare_value(mod, UINT64_C(0));
    }
    /*
     * always a <= b * s,
     * where a < 2^{64}, b >= 1, s >= 10^{20} > 2^{64}
     */
    assert(b != 0);
    return Sign::less_than;
}

static
Sign compare_with_exponent(std::uint64_t a, MpInt const& b, std::uint32_t exponent) {
    if (a == UINT64_C(0)) {
        if (b == UINT64_C(0)) {
            return Sign::equal_to;
        }
        return Sign::less_than;
    }
    if (b == UINT64_C(0)) {
        return Sign::greater_than;
    }
    if (exponent < s_compact_exponents.size()) {
        // (a <=> b*10^{exponent}) == (a / 10^{exponent} <=> b)
        std::uint64_t s = s_compact_exponents[exponent];
        std::uint64_t div = a / s;
        std::uint64_t mod = a % s;
        RETURN_IF_DIFFERENT(negate(b.compare_to(div)));
        return compare_value(mod, UINT64_C(0));
    }
    /*
     * always a <= b * s,
     * where a < 2^{64}, b >= 1, s >= 10^{20} > 2^{64}
     */
    assert(b != UINT64_C(0));
    return Sign::less_than;
}

static
Sign compare_with_exponent(MpInt const& a, std::uint64_t b, std::uint32_t exponent) {
    if (a == UINT64_C(0)) {
        return compare_value(UINT64_C(0), b);
    }
    if (b == UINT64_C(0)) {
        return Sign::greater_than;
    }
    return compare_with_exponent(a, MpInt(b), exponent);
}

inline
static std::tuple<std::uint32_t, std::uint32_t> predicate_product_bits(MpInt const& significand, std::uint32_t exponent) {
    if (significand == UINT64_C(0)) {
        return std::make_tuple(UINT32_C(0), UINT32_C(0));
    }
    std::uint32_t a_bits = significand.bits();

    /*
     * log_{2}(10) = 3.32...
     * 2^{3.3} < 10 < 2^{3.33..}
     * 2^{3.3n} < 10^{n} < 2^{10n/3}
     */
    std::uint32_t b_bits_min = static_cast<std::uint32_t>(std::floor(exponent * 3.3));
    std::uint32_t b_bits_max = (exponent * 10 + 2) / 3;

    /*
     * where X is a m-bits unsigned integer, where m>0, and
     * Y is a n-bits unsigned integer, where n>0.
     *
     * 2^{m-1} <= X < 2^{m},
     * 2^{n-1} <= Y < 2^{n},
     * 2^{n+m-2} <= X*Y < 2^{m+n}.
     *
     * X*Y must always have [n+m-1, n+m] bits.
     */
    return std::make_tuple<std::uint32_t, std::uint32_t>(a_bits + b_bits_min - 1, a_bits + b_bits_max);
}

/**
 * \brief Compares a <=> b*10^{exponent}
 */
static
Sign compare_with_exponent(MpInt const& a, MpInt const& b, std::uint32_t exponent) {
    if (exponent == UINT32_C(0)) {
        return a.compare_to(b);
    }
    if (a == UINT64_C(0)) {
        if (b == UINT64_C(0)) {
            return Sign::equal_to;
        }
        return Sign::less_than;
    }
    if (b == UINT64_C(0)) {
        return Sign::greater_than;
    }
    /*
     * where
     * X is a m-bits unsigned integer, where m>=0, and
     * Y is a n-bits unsigned integer, where n>=0.
     *
     * m < n --> X < Y
     * m > n --> X > Y
     */
    std::uint32_t a_bits = a.bits();
    auto b_bits_range = predicate_product_bits(b, exponent);
    if (a_bits < std::get<0>(b_bits_range)) {
        return Sign::less_than;
    }
    if (a_bits > std::get<1>(b_bits_range)) {
        return Sign::greater_than;
    }

    if (exponent <= 9) {
        // 10^9 < 2^{30}
        assert(s_compact_exponents[exponent] <= UINT64_C(0xffffffff));
        return a.compare_to(b * static_cast<std::uint32_t>(s_compact_exponents[exponent]));
    }
    MpInt b_product = b * MpInt::power_of_10(exponent);
    return a.compare_to(b_product);
}

} // namespace math
} // namespace asakusafw
