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
#include "serde.hpp"
#include "mpdecimal.hpp"

#include <iostream>
#include <memory>
#include <cassert>

extern "C" {
using namespace asakusafw::serde;

int32_t jna_compact_int_size(int8_t v) {
    auto r = compact_int_size(v);
    return r;
}

int64_t jna_read_compact_int(int8_t *p) {
    auto p0 = p;
    auto r = read_compact_int(p);
    assert(p == (p0 + compact_int_size(*p0)));
    return r;
}

#define DECL_JNA_COMPARE(T) int32_t jna_compare_##T(int8_t *a, int8_t *b) { \
    auto a0 = a; \
    auto b0 = b; \
    auto r = compare_##T(a, b); \
    if (r == 0) { \
        skip_##T(a0); \
        skip_##T(b0); \
        assert(a == a0); \
        assert(b == b0); \
    } \
    return r; \
}

DECL_JNA_COMPARE(boolean)
DECL_JNA_COMPARE(byte)
DECL_JNA_COMPARE(short)
DECL_JNA_COMPARE(int)
DECL_JNA_COMPARE(long)
DECL_JNA_COMPARE(float)
DECL_JNA_COMPARE(double)
DECL_JNA_COMPARE(date)
DECL_JNA_COMPARE(date_time)
DECL_JNA_COMPARE(string)
DECL_JNA_COMPARE(decimal)

#undef DECL_JNA_COMPARE

} // extern

extern "C" {

using namespace asakusafw::math;

static uint8_t *mpint_pack(const MpInt& v) {
    std::vector<uint8_t> vec = v.data();
    assert(vec.size() < 256);
    uint8_t *results = (uint8_t *) malloc(vec.size() + 1);
    results[0] = (uint8_t) vec.size();
    std::memcpy(&results[1], vec.data(), vec.size());
    return results;
}

void jna_free(void *ptr) {
    free(ptr);
}

uint8_t *jna_mpint(uint8_t *ptr, int64_t length) {
    MpInt v(ptr, (size_t) length);
    return mpint_pack(v);
}

uint8_t *jna_mpint_mult(uint8_t *ptr, int64_t length, uint32_t multiplier) {
    MpInt v(ptr, (size_t) length);
    return mpint_pack(v * multiplier);
}

uint8_t *jna_mpint_mult_mp(uint8_t *p0, int64_t l0, uint8_t *p1, int64_t l1) {
    MpInt a(p0, (size_t) l0);
    MpInt b(p1, (size_t) l1);
    return mpint_pack(a * b);
}

int32_t jna_mpint_cmp(uint8_t *p0, int64_t l0, int64_t b) {
    MpInt a(p0, (size_t) l0);
    return static_cast<int32_t>(a.compare_to(b));
}

int32_t jna_mpint_cmp_mp(uint8_t *p0, int64_t l0, uint8_t *p1, int64_t l1) {
    MpInt a(p0, (size_t) l0);
    MpInt b(p1, (size_t) l1);
    return static_cast<int32_t>(a.compare_to(b));
}

uint8_t *jna_mpint_pow_of_10(uint32_t exponent) {
    return mpint_pack(MpInt::power_of_10(exponent));
}
} // extern
