/*
 * Copyright 2011-2018 Asakusa Framework Team.
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
#include "com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl.h"
#include "mirror.hpp"
#include "jniutil.hpp"

/*
* Class:     com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl
* Method:    hasKey0
* Signature: (J)Z
*/
JNIEXPORT jboolean JNICALL Java_com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl_hasKey0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        OutputWriterMirror *self = (OutputWriterMirror *) _self;
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
* Class:     com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl
* Method:    getBaseOffset0
* Signature: (J)J
*/
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl_getBaseOffset0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        OutputWriterMirror *self = (OutputWriterMirror *) _self;
        return self->base_offset();
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
* Class:     com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl
* Method:    getContentsBuffer0
* Signature: (J)Ljava/nio/ByteBuffer;
*/
JNIEXPORT jobject JNICALL Java_com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl_getContentsBuffer0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        OutputWriterMirror *self = (OutputWriterMirror *) _self;
        return to_java_buffer(env, self->contents());
    } catch (JavaException &e) {
        e.rethrow(env);
        return nullptr;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return nullptr;
    }
}

/*
* Class:     com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl
* Method:    getEntryOffsetsBuffer0
* Signature: (J)Ljava/nio/ByteBuffer;
*/
JNIEXPORT jobject JNICALL Java_com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl_getEntryOffsetsBuffer0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        OutputWriterMirror *self = (OutputWriterMirror *) _self;
        return to_java_buffer(env, self->offsets());
    } catch (JavaException &e) {
        e.rethrow(env);
        return nullptr;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return nullptr;
    }
}

/*
* Class:     com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl
* Method:    getKeyLengthsBuffer0
* Signature: (J)Ljava/nio/ByteBuffer;
*/
JNIEXPORT jobject JNICALL Java_com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl_getKeyLengthsBuffer0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        OutputWriterMirror *self = (OutputWriterMirror *) _self;
        return to_java_buffer(env, self->key_lengths());
    } catch (JavaException &e) {
        e.rethrow(env);
        return nullptr;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return nullptr;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl
 * Method:    flush0
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl_flush0
(JNIEnv *env, jclass clazz, jlong _self, jint _recordCount) {
    try {
        OutputWriterMirror *self = (OutputWriterMirror *) _self;
        m3bp::size_type record_count = static_cast<m3bp::size_type>(_recordCount);
        self->flush(record_count);
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_OutputWriterMirrorImpl_close0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        OutputWriterMirror *self = (OutputWriterMirror *) _self;
        delete self;
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}
