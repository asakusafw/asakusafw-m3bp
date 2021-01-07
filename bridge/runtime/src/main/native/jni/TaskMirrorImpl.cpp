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
#include "com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl.h"
#include "mirror.hpp"
#include "jniutil.hpp"

using namespace asakusafw::jni;

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl
 * Method:    logicalTaskId0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl_logicalTaskId0
(JNIEnv *env, jclass, jlong _self) {
    try {
        auto* self = reinterpret_cast<TaskMirror*>(_self);
        auto id = static_cast<jlong>(self->logical_task_id());
        return id;
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl
 * Method:    physicalTaskId0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl_physicalTaskId0
(JNIEnv *env, jclass, jlong _self) {
    try {
        auto* self = reinterpret_cast<TaskMirror*>(_self);
        auto id = static_cast<jlong>(self->physical_task_id());
        return id;
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl
 * Method:    input0
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl_input0
(JNIEnv *env, jclass, jlong _self, jlong _id) {
    try {
        auto* self = reinterpret_cast<TaskMirror*>(_self);
        auto id = static_cast<m3bp::identifier_type>(_id);
        auto* result = self->input(id);
        return to_pointer(result);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl
 * Method:    output0
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl_output0
(JNIEnv *env, jclass, jlong _self, jlong _id) {
    try {
        auto* self = reinterpret_cast<TaskMirror*>(_self);
        auto id = static_cast<m3bp::identifier_type>(_id);
        auto* result = self->output(id);
        return to_pointer(result);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl
 * Method:    isCancelled0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_asakusafw_m3bp_mirror_jni_TaskMirrorImpl_isCancelled0
(JNIEnv *env, jclass, jlong _self) {
    try {
        auto* self = reinterpret_cast<TaskMirror*>(_self);
        return self->is_cancelled();
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}
