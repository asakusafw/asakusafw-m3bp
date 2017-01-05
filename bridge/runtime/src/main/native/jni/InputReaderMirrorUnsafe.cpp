/*
 * Copyright 2011-2017 Asakusa Framework Team.
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
#include "com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe.h"
#include <cstdint>
#include <cstring>
#include "mirror.hpp"
#include "jniutil.hpp"

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe
 * Method:    hasKey0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_hasKey0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        InputReaderMirror *self = (InputReaderMirror *) _self;
        return self->has_key();
    } catch (JavaException &e) {
        e.rethrow(env);
        return false;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return false;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe
 * Method:    compareBuffers0
 * Signature: (JJJ)I
 */
JNIEXPORT jint JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_compareBuffers0
(JNIEnv *, jclass, jlong _a, jlong _b, jlong _length) {
    void *a = (void *) _a;
    void *b = (void *) _b;
    size_t len = (size_t) _length;
    return (jint) memcmp(a, b, len);
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe
 * Method:    getInputBufferFragment0
 * Signature: (JZ[J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_getInputBufferFragment0
(JNIEnv *env, jclass clazz, jlong _self, jboolean is_key, jlongArray results) {
    try {
        jlong array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_VALUES_SIZE];
        InputReaderMirror *self = (InputReaderMirror *) _self;
        std::tuple<const void *, const void *, m3bp::size_type> buffer;
        if (is_key) {
            buffer = self->key_buffer();
        } else {
            buffer = self->value_buffer();
        }
        array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_INDEX_BUFFER_PTR] = (jlong) std::get<0>(buffer);
        array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_INDEX_OFFSET_TABLE_PTR] = (jlong) std::get<1>(buffer);
        array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_INDEX_RECORD_COUNT] = (jlong) std::get<2>(buffer);
        env->SetLongArrayRegion(results, 0, com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_VALUES_SIZE, &array[0]);
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorUnsafe_close0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        InputReaderMirror *self = (InputReaderMirror *) _self;
        delete self;
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}
