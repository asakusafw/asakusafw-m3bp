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
#include "com_asakusafw_m3bp_mirror_jni_VertexMirrorImpl.h"
#include "mirror.hpp"
#include "jniutil.hpp"

using namespace asakusafw::jni;

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_VertexMirrorImpl
 * Method:    createInput0
 * Signature: (JJLjava/lang/String;ILjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_VertexMirrorImpl_createInput0
(JNIEnv *env, jclass, jlong _self, jlong _id, jstring _name, jint _movement, jstring _comparator) {
    try {
        auto* self = reinterpret_cast<VertexMirror*>(_self);
        auto id = static_cast<m3bp::identifier_type>(_id);
        auto name = extract_string(env, _name);
        auto movement = static_cast<m3bp::Movement>(_movement);
        auto comparator = extract_string(env, _comparator);
        auto* port = self->input(id, name, movement, comparator);
        return to_pointer(port);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_VertexMirrorImpl
 * Method:    createOutput0
 * Signature: (JJLjava/lang/String;IZ)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_VertexMirrorImpl_createOutput0
(JNIEnv *env, jclass, jlong _self, jlong _id, jstring _name, jboolean has_key) {
    try {
        auto* self = reinterpret_cast<VertexMirror*>(_self);
        auto id = static_cast<m3bp::identifier_type>(_id);
        auto name = extract_string(env, _name);
        auto* port = self->output(id, name, static_cast<bool>(has_key));
        return to_pointer(port);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}
