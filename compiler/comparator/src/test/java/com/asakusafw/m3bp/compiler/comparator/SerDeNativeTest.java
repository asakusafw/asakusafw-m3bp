/**
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
package com.asakusafw.m3bp.compiler.comparator;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.DataOutput;
import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.runtime.io.ValueOptionSerDe;
import com.asakusafw.runtime.value.BooleanOption;
import com.asakusafw.runtime.value.ByteOption;
import com.asakusafw.runtime.value.Date;
import com.asakusafw.runtime.value.DateOption;
import com.asakusafw.runtime.value.DateTime;
import com.asakusafw.runtime.value.DateTimeOption;
import com.asakusafw.runtime.value.DecimalOption;
import com.asakusafw.runtime.value.DoubleOption;
import com.asakusafw.runtime.value.FloatOption;
import com.asakusafw.runtime.value.IntOption;
import com.asakusafw.runtime.value.LongOption;
import com.asakusafw.runtime.value.ShortOption;
import com.asakusafw.runtime.value.StringOption;
import com.asakusafw.runtime.value.ValueOption;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

/**
 * Test for {@code "native/include/serde.hpp"}.
 */
public class SerDeNativeTest {

    static final Logger LOG = LoggerFactory.getLogger(SerDeNativeTest.class);

    static final Mapper MAPPER;
    static {
        String path = new File("target/native/test/lib").getAbsolutePath();
        NativeLibrary.addSearchPath("test-serde", path);
        Mapper mapper;
        try {
            mapper = (Mapper) Native.loadLibrary("test-serde", Mapper.class);
        } catch (LinkageError e) {
            LOG.warn("native library is not available", e);
            mapper = null;
        }
        MAPPER = mapper;
    }

    /**
     * Check whether native library is enabled.
     */
    @ClassRule
    public static final TestRule NATIVE_CHECKER = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            Assume.assumeNotNull(MAPPER);
        }
    };

    /**
     * Test for {@code compare_boolean}.
     * @throws Exception if failed
     */
    @Test
    public void compare_boolean() throws Exception {
        Comparator<BooleanOption> cmp = comparator(MAPPER::jna_compare_boolean);

        compare(cmp, new BooleanOption(false), new BooleanOption(false));
        compare(cmp, new BooleanOption(true), new BooleanOption(true));
        compare(cmp, new BooleanOption(true), new BooleanOption(false));
        compare(cmp, new BooleanOption(false), new BooleanOption(true));

        compare(cmp, new BooleanOption(), new BooleanOption());
        compare(cmp, new BooleanOption(false), new BooleanOption());
        compare(cmp, new BooleanOption(), new BooleanOption(false));
    }

    /**
     * Test for {@code compare_byte}.
     * @throws Exception if failed
     */
    @Test
    public void compare_byte() throws Exception {
        Comparator<ByteOption> cmp = comparator(MAPPER::jna_compare_byte);

        compare(cmp, new ByteOption((byte) 0), new ByteOption((byte) 0));
        compare(cmp, new ByteOption((byte) 1), new ByteOption((byte) 0));
        compare(cmp, new ByteOption((byte) 0), new ByteOption((byte) 1));

        compare(cmp, new ByteOption(), new ByteOption());
        compare(cmp, new ByteOption((byte) -1), new ByteOption());
        compare(cmp, new ByteOption(), new ByteOption((byte) -1));
    }

    /**
     * Test for {@code compare_short}.
     * @throws Exception if failed
     */
    @Test
    public void compare_short() throws Exception {
        Comparator<ShortOption> cmp = comparator(MAPPER::jna_compare_short);

        compare(cmp, new ShortOption((short) 0), new ShortOption((short) 0));
        compare(cmp, new ShortOption((short) 1), new ShortOption((short) 0));
        compare(cmp, new ShortOption((short) 0), new ShortOption((short) 1));

        compare(cmp, new ShortOption(), new ShortOption());
        compare(cmp, new ShortOption((short) -1), new ShortOption());
        compare(cmp, new ShortOption(), new ShortOption((short) -1));
    }

    /**
     * Test for {@code compare_int}.
     * @throws Exception if failed
     */
    @Test
    public void compare_int() throws Exception {
        Comparator<IntOption> cmp = comparator(MAPPER::jna_compare_int);

        compare(cmp, new IntOption(0), new IntOption(0));
        compare(cmp, new IntOption(1), new IntOption(0));
        compare(cmp, new IntOption(0), new IntOption(1));

        compare(cmp, new IntOption(), new IntOption());
        compare(cmp, new IntOption(-1), new IntOption());
        compare(cmp, new IntOption(), new IntOption(-1));
    }

    /**
     * Test for {@code compare_long}.
     * @throws Exception if failed
     */
    @Test
    public void compare_long() throws Exception {
        Comparator<LongOption> cmp = comparator(MAPPER::jna_compare_long);

        compare(cmp, new LongOption(0), new LongOption(0));
        compare(cmp, new LongOption(1), new LongOption(0));
        compare(cmp, new LongOption(0), new LongOption(1));

        compare(cmp, new LongOption(), new LongOption());
        compare(cmp, new LongOption(-1), new LongOption());
        compare(cmp, new LongOption(), new LongOption(-1));
    }

    /**
     * Test for {@code compare_float}.
     * @throws Exception if failed
     */
    @Test
    public void compare_float() throws Exception {
        Comparator<FloatOption> cmp = comparator(MAPPER::jna_compare_float);

        compare(cmp, new FloatOption(0), new FloatOption(0));
        compare(cmp, new FloatOption(1), new FloatOption(0));
        compare(cmp, new FloatOption(0), new FloatOption(1));

        compare(cmp, new FloatOption(), new FloatOption());
        compare(cmp, new FloatOption(-1), new FloatOption());
        compare(cmp, new FloatOption(), new FloatOption(-1));
    }

    /**
     * Test for {@code compare_double}.
     * @throws Exception if failed
     */
    @Test
    public void compare_double() throws Exception {
        Comparator<DoubleOption> cmp = comparator(MAPPER::jna_compare_double);

        compare(cmp, new DoubleOption(0), new DoubleOption(0));
        compare(cmp, new DoubleOption(1), new DoubleOption(0));
        compare(cmp, new DoubleOption(0), new DoubleOption(1));

        compare(cmp, new DoubleOption(), new DoubleOption());
        compare(cmp, new DoubleOption(-1), new DoubleOption());
        compare(cmp, new DoubleOption(), new DoubleOption(-1));
    }

    /**
     * Test for {@code compare_date}.
     * @throws Exception if failed
     */
    @Test
    public void compare_date() throws Exception {
        Comparator<DateOption> cmp = comparator(MAPPER::jna_compare_date);

        compare(cmp, newDate(0), newDate(0));
        compare(cmp, newDate(1), newDate(0));
        compare(cmp, newDate(0), newDate(1));

        compare(cmp, new DateOption(), new DateOption());
        compare(cmp, newDate(0), new DateOption());
        compare(cmp, new DateOption(), newDate(0));
    }

    /**
     * Test for {@code compare_date_time}.
     * @throws Exception if failed
     */
    @Test
    public void compare_date_time() throws Exception {
        Comparator<DateTimeOption> cmp = comparator(MAPPER::jna_compare_date_time);

        compare(cmp, newDateTime(0), newDateTime(0));
        compare(cmp, newDateTime(1), newDateTime(0));
        compare(cmp, newDateTime(0), newDateTime(1));

        compare(cmp, new DateTimeOption(), new DateTimeOption());
        compare(cmp, newDateTime(0), new DateTimeOption());
        compare(cmp, new DateTimeOption(), newDateTime(0));
    }

    /**
     * Test for {@code compare_string}.
     * @throws Exception if failed
     */
    @Test
    public void compare_string() throws Exception {
        Comparator<StringOption> cmp = comparator(MAPPER::jna_compare_string);

        compare(cmp, new StringOption("a"), new StringOption("a"));
        compare(cmp, new StringOption("b"), new StringOption("a"));
        compare(cmp, new StringOption("a"), new StringOption("b"));

        compare(cmp, new StringOption("AAA"), new StringOption("AAA"));
        compare(cmp, new StringOption("ABA"), new StringOption("AAB"));
        compare(cmp, new StringOption("AAB"), new StringOption("ABA"));

        compare(cmp, new StringOption(), new StringOption());
        compare(cmp, new StringOption("a"), new StringOption());
        compare(cmp, new StringOption(), new StringOption("a"));
    }

    /**
     * Test for {@code compare_decimal}.
     * @throws Exception if failed
     */
    @Test
    public void compare_decimal() throws Exception {
        Comparator<DecimalOption> cmp = comparator(MAPPER::jna_compare_decimal);

        compare(cmp, newDecimal("1"), newDecimal("1"));
        compare(cmp, newDecimal("1.1"), newDecimal("1"));
        compare(cmp, newDecimal("1.10"), newDecimal("1"));
        compare(cmp, newDecimal("1.10"), newDecimal("2"));
        compare(cmp, newDecimal("1"), newDecimal("1.1"));
        compare(cmp, newDecimal("1"), newDecimal("1.10"));
        compare(cmp, newDecimal("2"), newDecimal("1.10"));

        compare(cmp, newDecimal("1"), newDecimal("1"));
        compare(cmp, newDecimal("1"), newDecimal("-1"));
        compare(cmp, newDecimal("-1"), newDecimal("1"));

        compare(cmp, new DecimalOption(), new DecimalOption());
        compare(cmp, newDecimal("1.1"), new DecimalOption());
        compare(cmp, new DecimalOption(), newDecimal("1.1"));
    }

    private DateOption newDate(int v) {
        return new DateOption(new Date(v));
    }

    private DateTimeOption newDateTime(long v) {
        return new DateTimeOption(new DateTime(v));
    }

    private DecimalOption newDecimal(String v) {
        return new DecimalOption(new BigDecimal(v));
    }

    private static <T extends Comparable<? super T>> void compare(Comparator<T> cmp, T a, T b) {
        int sign = a.compareTo(b);
        int result = cmp.compare(a, b);
        if (sign == 0) {
            assertThat(result, equalTo(0));
        } else if (sign < 0) {
            assertThat(result, lessThan(0));
        } else {
            assertThat(result, greaterThan(0));
        }
    }

    private <T extends ValueOption<?>> Comparator<T> comparator(BiFunction<Pointer, Pointer, Integer> func) {
        return (a, b) -> {
            Pointer pa = serialize(a);
            Pointer pb = serialize(b);
            return func.apply(pa, pb);
        };
    }

    private Pointer serialize(ValueOption<?> v) {
        try {
            Memory memory = new Memory(1024);
            ByteBuffer buffer = memory.getByteBuffer(0, memory.size());
            ByteBufferDataOutput output = new ByteBufferDataOutput(buffer);

            Class<?> type = v.getClass();
            Method target = ValueOptionSerDe.class.getMethod("serialize", type, DataOutput.class);
            target.invoke(null, v, output);

            return memory;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * mpint - simple case.
     */
    @Test
    public void mpint_simple() {
        Memory buf = bytes(100);
        BigInteger r = toBigInt(() -> MAPPER.jna_mpint(buf, buf.size()));
        assertThat(r, is(toBigInt("100")));
    }

    /**
     * mpint - multiple words.
     */
    @Test
    public void mpint_multiple_words() {
        Memory buf = bytes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        byte[] r = toBytes(() -> MAPPER.jna_mpint(buf, size(buf)));
        assertThat(r, is(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }));
    }

    /**
     * mpint - leading zeros.
     */
    @Test
    public void mpint_leading_zeros() {
        Memory buf = bytes(0, 0, 0, 0, 0, 1, 2, 3);
        byte[] r = toBytes(() -> MAPPER.jna_mpint(buf, size(buf)));
        assertThat(r, is(new byte[] { 1, 2, 3 }));
    }

    /**
     * mpint - just zero.
     */
    @Test
    public void mpint_zero() {
        byte[] r = toBytes(() -> MAPPER.jna_mpint(Pointer.NULL, 0));
        assertThat(r, is(new byte[0]));
    }

    /**
     * mpint - zero w/ leading zeros.
     */
    @Test
    public void mpint_zero_leading_zeros() {
        Memory buf = bytes(0);
        byte[] r = toBytes(() -> MAPPER.jna_mpint(buf, size(buf)));
        assertThat(r, is(new byte[0]));
    }

    /**
     * mpint - multiply.
     */
    @Test
    public void mpint_mult() {
        BigInteger a = toBigInt("12398530985723743985");
        int b = 2;
        BigInteger c = mpintMult(a, b);
        assertThat(c, is(a.multiply(BigInteger.valueOf(b))));
    }

    /**
     * mpint - multiply.
     */
    @Test
    public void mpint_mult_mp() {
        BigInteger a = toBigInt("1000000");
        BigInteger b = toBigInt("1000000");
        BigInteger c = mpintMult(a, b);
        assertThat(c, is(a.multiply(b)));
    }

    /**
     * mpint - multiply.
     */
    @Test
    public void mpint_mult_mp_huge() {
        BigInteger a = toBigInt("378238523420943283472343782");
        BigInteger b = toBigInt("124766129372357435248");
        BigInteger c = mpintMult(a, b);
        assertThat(c, is(a.multiply(b)));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_w0() {
        assertThat(mpintCompare("0", 0), equalTo(0));
        assertThat(mpintCompare("0", 1), lessThan(0));
        assertThat(mpintCompare("0", 0x12_3456_789aL), lessThan(0));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_w1() {
        assertThat(mpintCompare("100", 0), greaterThan(0));
        assertThat(mpintCompare("100", 1), greaterThan(0));
        assertThat(mpintCompare("100", 99), greaterThan(0));
        assertThat(mpintCompare("100", 100), equalTo(0));
        assertThat(mpintCompare("100", 101), lessThan(0));
        assertThat(mpintCompare("100", 0xffff_ffffL), lessThan(0));
        assertThat(mpintCompare("100", 0x1_0000_0000L), lessThan(0));
        assertThat(mpintCompare("100", 0x12_3456_789aL), lessThan(0));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_w2() {
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 0), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 100), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 0xffff_ffffL), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 0x1_0000_0000L), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 0x1234_5678_9abc_def0L), equalTo(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 0x1234_5678_9abc_def1L), lessThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 0x7fff_ffff_ffff_ffffL), lessThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0", 0xffff_ffff_ffff_ffffL), lessThan(0));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_w3() {
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", 0), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", 100), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", 0xffff_ffffL), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", 0x1_0000_0000L), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", 0x7fff_ffff_ffff_ffffL), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", 0xffff_ffff_ffff_ffffL), greaterThan(0));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_mp_w0() {
        assertThat(mpintCompare("0", "0"), equalTo(0));
        assertThat(mpintCompare("0", "1"), lessThan(0));
        assertThat(mpintCompare("0", "0x1234_5678_9abc"), lessThan(0));
        assertThat(mpintCompare("0", "0x1234_5678_9abc_def0_1234"), lessThan(0));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_mp_w1() {
        assertThat(mpintCompare("0x1234", "0"), greaterThan(0));
        assertThat(mpintCompare("0x1234", "0x1233"), greaterThan(0));
        assertThat(mpintCompare("0x1234", "0x1234"), equalTo(0));
        assertThat(mpintCompare("0x1234", "0x1235"), lessThan(0));
        assertThat(mpintCompare("0x1234", "0x1234_5678_9abc"), lessThan(0));
        assertThat(mpintCompare("0x1234", "0x1234_5678_9abc_def0_1234"), lessThan(0));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_mp_w2() {
        assertThat(mpintCompare("0x1234_5678_9abc", "0"), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc", "0x1234"), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc", "0x1234_5678_9abb"), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc", "0x1234_5678_9abc"), equalTo(0));
        assertThat(mpintCompare("0x1234_5678_9abc", "0x1234_5678_9abd"), lessThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc", "0x1234_5678_9abc_def0_1234"), lessThan(0));
    }

    /**
     * mpint - compare.
     */
    @Test
    public void mpint_compare_mp_w3() {
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0"), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0x1234"), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0x1234_5678_9abc"), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0x1234_5678_9abc_def0_1233"), greaterThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0x1234_5678_9abc_def0_1234"), equalTo(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0x1234_5678_9abc_def0_1235"), lessThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0x1234_5678_9abc_def0_1234_0"), lessThan(0));
        assertThat(mpintCompare("0x1234_5678_9abc_def0_1234", "0x1234_5678_9abc_def0_1234_5678_9abc"), lessThan(0));
    }

    /**
     * mpint - 10^n.
     */
    @Test
    public void mpint_pow_of_10() {
        for (int i = 0; i < 100; i++) {
            int exp = i;
            BigInteger r = toBigInt(() -> MAPPER.jna_mpint_pow_of_10(exp));
            assertThat(r, is(BigInteger.TEN.pow(exp)));
        }
    }

    private BigInteger mpintMult(BigInteger a, int b) {
        Memory p = toMpInt(a);
        try (ClosablePointer c = new ClosablePointer(MAPPER.jna_mpint_mult(p, size(p), b))) {
            return toBigInt(c);
        }
    }

    private BigInteger mpintMult(BigInteger a, BigInteger b) {
        Memory pA = toMpInt(a);
        Memory pB = toMpInt(b);
        try (ClosablePointer c = new ClosablePointer(MAPPER.jna_mpint_mult_mp(pA, size(pA), pB, size(pB)))) {
            return toBigInt(c);
        }
    }

    private int mpintCompare(String a, long b) {
        Memory m = toMpInt(toBigInt(a));
        return MAPPER.jna_mpint_cmp(m, size(m), b);
    }

    private long size(Pointer ptr) {
        if (ptr instanceof Memory) {
            return ((Memory) ptr).size();
        } else if (ptr == Pointer.NULL || ptr == null) {
            return 0;
        } else {
            throw new AssertionError();
        }
    }

    private int mpintCompare(String a, String b) {
        Memory p0 = toMpInt(toBigInt(a));
        Memory p1 = toMpInt(toBigInt(b));
        return MAPPER.jna_mpint_cmp_mp(p0, size(p0), p1, size(p1));
    }

    private BigInteger toBigInt(String s) {
        String v = s.replaceAll("_", "");
        boolean hex = false;
        if (v.startsWith("0x")) {
            hex = true;
            v = v.substring(2);
        }
        return new BigInteger(v, hex ? 16 : 10);
    }

    private Memory bytes(int... bytes) {
        Memory memory = new Memory(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            memory.setByte(i, (byte) bytes[i]);
        }
        return memory;
    }

    private Memory toMpInt(BigInteger value) {
        if (value.equals(BigInteger.ZERO)) {
            return null;
        }
        assertThat(value.signum(), is(1));
        byte[] bytes = value.toByteArray();
        Memory memory = new Memory(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            memory.setByte(i, bytes[i]);
        }
        return memory;
    }

    private BigInteger toBigInt(Supplier<Pointer> s) {
        try (ClosablePointer c = new ClosablePointer(s.get())) {
            return toBigInt(c);
        }
    }

    private byte[] toBytes(Supplier<Pointer> s) {
        try (ClosablePointer c = new ClosablePointer(s.get())) {
            return toBytes(c);
        }
    }

    private byte[] toBytes(Pointer ptr) {
        int size = ptr.getByte(0) & 0xff;
        return ptr.getByteArray(Byte.BYTES, size);
    }

    private BigInteger toBigInt(Pointer ptr) {
        byte[] mpint = toBytes(ptr);
        return new BigInteger(1, mpint);
    }

    /**
     * Test for {@code compare_decimal} w/ {compact, compact}.
     * @throws Exception if failed
     */
    @Test
    public void compare_decimal_compact_compact() throws Exception {
        assertCompare("0", 0, "0", 0, 0);
        assertCompare("0", 0, "0", 10, 0);

        assertCompare("0", 0, "1", 0, -1);
        assertCompare("0", 0, "1", 10, -1);

        assertCompare("1", 10, "1", 10, 0);
        assertCompare("1", 10, "2", 10, -1);
        assertCompare("2", 10, "1", 10, +1);

        assertCompare("1", 10, "10", 9, 0);
        assertCompare("1", 10, "11", 9, -1);
        assertCompare("1", 10, "_9", 9, +1);

        assertCompare("1", 10, "10000000000", 0, 0);
        assertCompare("1", 10, "10000000001", 0, -1);
        assertCompare("1", 10, "_9999999999", 0, +1);

        assertCompare("1", 18, "1000000000000000000", 0, 0);
        assertCompare("1", 18, "1000000000100000000", 0, -1);
        assertCompare("1", 18, "_999999999900000000", 0, +1);

        assertCompare("1", 20, "9223372036854775807", 0, +1);
        assertCompare("1", 20, "9223372036854775807", 2, -1);
    }

    /**
     * Test for {@code compare_decimal} w/ (compact, mp).
     * @throws Exception if failed
     */
    @Test
    public void compare_decimal_compact_mp() throws Exception {
        int ePad = 20;
        String sPad = Stream.generate(() -> "0").limit(ePad).collect(Collectors.joining());

        assertCompare("0", 0 + ePad, "0" + sPad, 0, 0);
        assertCompare("0", 0 + ePad, "0" + sPad, 10, 0);

        assertCompare("1", 10 + ePad, "1" + sPad, 10, 0);
        assertCompare("1", 10 + ePad, "2" + sPad, 10, -1);
        assertCompare("2", 10 + ePad, "1" + sPad, 10, +1);

        assertCompare("1", 10 + ePad, "10" + sPad, 9, 0);
        assertCompare("1", 10 + ePad, "11" + sPad, 9, -1);
        assertCompare("1", 10 + ePad, "_9" + sPad, 9, +1);

        assertCompare("1", 10 + ePad, "10000000000" + sPad, 0, 0);
        assertCompare("1", 10 + ePad, "10000000001" + sPad, 0, -1);
        assertCompare("1", 10 + ePad, "_9999999999" + sPad, 0, +1);

        assertCompare("1", 18 + ePad, "1000000000000000000" + sPad, 0, 0);
        assertCompare("1", 18 + ePad, "1000000000100000000" + sPad, 0, -1);
        assertCompare("1", 18 + ePad, "_999999999900000000" + sPad, 0, +1);

        assertCompare("1", 20 + ePad, "9223372036854775807" + sPad, 0, +1);
        assertCompare("1", 20 + ePad, "9223372036854775807" + sPad, 2, -1);
    }

    /**
     * Test for {@code compare_decimal} w/ (compact, mp).
     * @throws Exception if failed
     */
    @Test
    public void compare_decimal_mp_compact() throws Exception {
        int ePad = 20;
        String sPad = Stream.generate(() -> "0").limit(ePad).collect(Collectors.joining());

        assertCompare("1" + sPad, 10, "1", 10 + ePad, 0);
        assertCompare("1" + sPad, 10, "2", 10 + ePad, -1);
        assertCompare("2" + sPad, 10, "1", 10 + ePad, +1);

        assertCompare("1" + sPad, 10, "10", 9 + ePad, 0);
        assertCompare("1" + sPad, 10, "11", 9 + ePad, -1);
        assertCompare("1" + sPad, 10, "_9", 9 + ePad, +1);

        assertCompare("1" + sPad, 10, "10000000000", 0 + ePad, 0);
        assertCompare("1" + sPad, 10, "10000000001", 0 + ePad, -1);
        assertCompare("1" + sPad, 10, "_9999999999", 0 + ePad, +1);

        assertCompare("1" + sPad, 18, "1000000000000000000", 0 + ePad, 0);
        assertCompare("1" + sPad, 18, "1000000000100000000", 0 + ePad, -1);
        assertCompare("1" + sPad, 18, "_999999999900000000", 0 + ePad, +1);

        assertCompare("1" + sPad, 20, "9223372036854775807", 0 + ePad, +1);
        assertCompare("1" + sPad, 20, "9223372036854775807", 2 + ePad, -1);
    }

    /**
     * Test for {@code compare_decimal} w/ {mp, mp}.
     * @throws Exception if failed
     */
    @Test
    public void compare_decimal_mp_mp() throws Exception {
        int ePad = 20;
        String sPad = Stream.generate(() -> "0").limit(ePad).collect(Collectors.joining());

        assertCompare("0", 0, "1" + sPad, -ePad, -1);

        assertCompare("1" + sPad, 10, "1" + sPad, 10, 0);
        assertCompare("1" + sPad, 10, "2" + sPad, 10, -1);
        assertCompare("2" + sPad, 10, "1" + sPad, 10, +1);

        assertCompare("1" + sPad, 10, "10" + sPad, 9, 0);
        assertCompare("1" + sPad, 10, "11" + sPad, 9, -1);
        assertCompare("1" + sPad, 10, "_9" + sPad, 9, +1);

        assertCompare("1" + sPad, 10, "10000000000" + sPad, 0, 0);
        assertCompare("1" + sPad, 10, "10000000001" + sPad, 0, -1);
        assertCompare("1" + sPad, 10, "_9999999999" + sPad, 0, +1);

        assertCompare("1" + sPad, 18, "1000000000000000000" + sPad, 0, 0);
        assertCompare("1" + sPad, 18, "1000000000100000000" + sPad, 0, -1);
        assertCompare("1" + sPad, 18, "_999999999900000000" + sPad, 0, +1);

        assertCompare("1" + sPad, 20, "9223372036854775807" + sPad, 0, +1);
        assertCompare("1" + sPad, 20, "9223372036854775807" + sPad, 2, -1);

        assertCompare("0x1234_12345678_12345678", 0, "0x__34_12345678_12345678", 0, +1);
    }

    private void assertCompare(String s0, int e0, String s1, int e1, int sign) {
        DecimalOption d0 = newDecimal(s0, e0);
        DecimalOption d1 = newDecimal(s1, e1);
        assertCompare0(d0, d1, sign);
        assertCompare0(d1, d0, -sign);
    }

    private void assertCompare0(DecimalOption a, DecimalOption b, int sign) {
        Comparator<DecimalOption> cmp = comparator(MAPPER::jna_compare_decimal);
        Matcher<Integer> result = sign == 0 ? equalTo(0) : sign < 0 ? lessThan(0) : greaterThan(0);
        assertThat(cmp.compare(a, b), result);
    }

    // significand * 10^{exponent}
    private DecimalOption newDecimal(String significand, int exponent) {
        BigInteger sig = toBigInt(significand);
        return new DecimalOption(new BigDecimal(sig, -exponent));
    }

    /**
     * native mapper.
     */
    @SuppressWarnings("javadoc")
    public interface Mapper extends Library {
        int jna_compact_int_size(byte head);
        long jna_read_compact_int(Pointer bytes);

        int jna_compare_boolean(Pointer a, Pointer b);
        int jna_compare_byte(Pointer a, Pointer b);
        int jna_compare_short(Pointer a, Pointer b);
        int jna_compare_int(Pointer a, Pointer b);
        int jna_compare_long(Pointer a, Pointer b);
        int jna_compare_float(Pointer a, Pointer b);
        int jna_compare_double(Pointer a, Pointer b);
        int jna_compare_date(Pointer a, Pointer b);
        int jna_compare_date_time(Pointer a, Pointer b);
        int jna_compare_string(Pointer a, Pointer b);
        int jna_compare_decimal(Pointer a, Pointer b);

        Pointer jna_mpint(Pointer buf, long length);

        Pointer jna_mpint_mult(Pointer buf, long length, int multiplier);
        Pointer jna_mpint_mult_mp(Pointer b0, long l0, Pointer b1, long l1);

        int jna_mpint_cmp(Pointer buf, long length, long other);
        int jna_mpint_cmp_mp(Pointer b0, long l0, Pointer b1, long l1);

        Pointer jna_mpint_pow_of_10(int exponent);

        void jna_free(Pointer ptr);
    }

    private class ClosablePointer extends Pointer implements AutoCloseable {

        ClosablePointer(Pointer pointer) {
            super(Pointer.nativeValue(pointer));
        }

        @Override
        public void close() {
            MAPPER.jna_free(this);
        }
    }
}
