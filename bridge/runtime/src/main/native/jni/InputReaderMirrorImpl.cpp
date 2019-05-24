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
#include "com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl.h"
#include "mirror.hpp"
#include "jniutil.hpp"

using namespace asakusafw::jni;

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
 * Method:    hasKey0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_hasKey0
(JNIEnv *env, jclass, jlong _self) {
    try {
        auto* self = reinterpret_cast<InputReaderMirror*>(_self);
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
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
 * Method:    getInputBufferFragment0
 * Signature: (JZ[J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_getInputBufferFragment0
(JNIEnv *env, jclass, jlong _self, jboolean is_key, jlongArray results) {
    try {
        auto* self = reinterpret_cast<InputReaderMirror*>(_self);
        jlong array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_VALUES_SIZE];
        std::tuple<const void *, const void *, m3bp::size_type> buffer;
        if (is_key) {
            buffer = self->key_buffer();
        } else {
            buffer = self->value_buffer();
        }
        array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_INDEX_BUFFER_PTR] = to_pointer(std::get<0>(buffer));
        array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_INDEX_OFFSET_TABLE_PTR] = to_pointer(std::get<1>(buffer));
        array[com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_INDEX_RECORD_COUNT] = static_cast<jlong>(std::get<2>(buffer));
        env->SetLongArrayRegion(results, 0, com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_VALUES_SIZE, &array[0]);
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_close0
(JNIEnv *env, jclass, jlong _self) {
    try {
        auto* self = reinterpret_cast<InputReaderMirror*>(_self);
        delete self;
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}
