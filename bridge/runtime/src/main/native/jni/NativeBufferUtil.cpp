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
#include "com_asakusafw_m3bp_mirror_jni_NativeBufferUtil.h"
#include <cstring>

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_NativeBufferUtil
 * Method:    getView0
 * Signature: (JI)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_asakusafw_m3bp_mirror_jni_NativeBufferUtil_getView0
(JNIEnv* env, jclass, jlong ptr, jint length) {
    return env->NewDirectByteBuffer(reinterpret_cast<void*>(ptr), length);
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_NativeBufferUtil
 * Method:    getAddress0
 * Signature: (Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_NativeBufferUtil_getAddress0
(JNIEnv* env, jclass, jobject buffer) {
    return reinterpret_cast<jlong>(env->GetDirectBufferAddress(buffer));
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_NativeBufferUtil
 * Method:    compare0
 * Signature: (JJJ)I
 */
JNIEXPORT jint JNICALL Java_com_asakusafw_m3bp_mirror_jni_NativeBufferUtil_compare0
(JNIEnv *, jclass, jlong _a, jlong _b, jlong _length) {
    auto* a = reinterpret_cast<void*>(_a);
    auto* b = reinterpret_cast<void*>(_b);
    auto len = static_cast<std::size_t>(_length);
    return static_cast<jint>(std::memcmp(a, b, len));
}
