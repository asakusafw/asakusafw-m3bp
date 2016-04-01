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
#include "com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl.h"
#include <algorithm>
#include <cstdint>
#include <cstring>
#include "mirror.hpp"
#include "jniutil.hpp"

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
 * Method:    hasKey0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_hasKey0
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
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
 * Method:    getBaseOffset0
 * Signature: (JZ)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_getBaseOffset0
(JNIEnv *env, jclass clazz, jlong _self, jboolean is_key) {
    try {
        InputReaderMirror *self = (InputReaderMirror *) _self;
        if (is_key) {
            return self->key_base_offset();
        } else {
            return self->value_base_offset();
        }
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
 * Method:    compareBuffers0
 * Signature: (Ljava/nio/ByteBuffer;IILjava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_compareBuffers0
(JNIEnv *env, jclass clazz, jobject buf0, jint off0, jint len0, jobject buf1, jint off1, jint len1) {
    try {
        uint8_t *p0 = (uint8_t *) env->GetDirectBufferAddress(buf0);
        uint8_t *p1 = (uint8_t *) env->GetDirectBufferAddress(buf1);
        size_t len = std::min(len0, len1);
        int diff = memcmp(p0 + off0, p1 + off1, len);
        if (diff) {
            return diff;
        }
        if (len0 == len1) {
            return 0;
        } else if (len0 < len1) {
            return -1;
        } else {
            return +1;
        }
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
* Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
* Method:    getContentsBuffer0
* Signature: (JZ)Ljava/nio/ByteBuffer;
*/
JNIEXPORT jobject JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_getContentsBuffer0
(JNIEnv *env, jclass clazz, jlong _self, jboolean is_key) {
    try {
        InputReaderMirror *self = (InputReaderMirror *) _self;
        if (is_key) {
            return to_java_buffer(env, self->key_contents());
        } else {
            return to_java_buffer(env, self->value_contents());
        }
    } catch (JavaException &e) {
        e.rethrow(env);
        return nullptr;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return nullptr;
    }
}

/*
* Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
* Method:    getEntryOffsetsBuffer0
* Signature: (JZ)Ljava/nio/ByteBuffer;
*/
JNIEXPORT jobject JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_getEntryOffsetsBuffer0
(JNIEnv *env, jclass clazz, jlong _self, jboolean is_key) {
    try {
        InputReaderMirror *self = (InputReaderMirror *) _self;
        if (is_key) {
            return to_java_buffer(env, self->key_offsets());
        } else {
            return to_java_buffer(env, self->value_offsets());
        }
    } catch (JavaException &e) {
        e.rethrow(env);
        return nullptr;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return nullptr;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl
 * Method:    advance0
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_advance0
(JNIEnv *env, jclass clazz, jlong _self, jboolean is_key) {
    try {
        InputReaderMirror *self = (InputReaderMirror *) _self;
        if (is_key) {
            return self->advance_key_buffer();
        } else {
            return self->advance_value_buffer();
        }
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
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_InputReaderMirrorImpl_close0
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
