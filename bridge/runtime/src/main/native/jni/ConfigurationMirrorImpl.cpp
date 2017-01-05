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
#include "com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl.h"
#include "mirror.hpp"
#include "jniutil.hpp"

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    getMaxConcurrency0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_getMaxConcurrency0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        return static_cast<jlong>(self->entity().max_concurrency());
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    setMaxConcurrency0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_setMaxConcurrency0
(JNIEnv *env, jclass clazz, jlong _self, jlong value) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        self->entity().max_concurrency(static_cast<unsigned int>(value));
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    getPartitionCount0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_getPartitionCount0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        return static_cast<jlong>(self->entity().partition_count());
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    setPartitionCount0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_setPartitionCount0
(JNIEnv *env, jclass clazz, jlong _self, jlong value) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        self->entity().partition_count(static_cast<m3bp::size_type>(value));
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    getOutputBufferSize0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_getOutputBufferSize0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        return static_cast<jlong>(self->entity().default_output_buffer_size());
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    setOutputBufferSize0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_setOutputBufferSize0
(JNIEnv *env, jclass clazz, jlong _self, jlong value) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        self->entity().default_output_buffer_size(static_cast<m3bp::size_type>(value));
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    getOutputRecordsPerBuffer0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_getOutputRecordsPerBuffer0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        return static_cast<jlong>(self->entity().default_records_per_buffer());
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    setOutputRecordsPerBuffer0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_setOutputRecordsPerBuffer0
(JNIEnv *env, jclass clazz, jlong _self, jlong value) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        self->entity().default_records_per_buffer(static_cast<m3bp::size_type>(value));
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    getAffinityMode0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_getAffinityMode0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        return static_cast<jint>(self->entity().affinity());
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    setAffinityMode0
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_setAffinityMode0
(JNIEnv *env, jclass clazz, jlong _self, jint value) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        self->entity().affinity(static_cast<m3bp::AffinityMode>(value));
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    getProfilingOutput0
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_getProfilingOutput0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        std::string str(self->entity().profile_log());
        if (str.empty()) {
            return nullptr;
        } else {
            jbyteArray results = env->NewByteArray(str.length());
            env->SetByteArrayRegion(results, 0, str.length(), (const jbyte *) str.data());
            return results;
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
 * Class:     com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl
 * Method:    setProfilingOutput0
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_ConfigurationMirrorImpl_setProfilingOutput0
(JNIEnv *env, jclass clazz, jlong _self, jstring _value) {
    try {
        ConfigurationMirror *self = (ConfigurationMirror *) _self;
        const char *value = env->GetStringUTFChars(_value, 0);
        self->entity().profile_log(std::string(value));
        env->ReleaseStringUTFChars(_value, value);
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}
