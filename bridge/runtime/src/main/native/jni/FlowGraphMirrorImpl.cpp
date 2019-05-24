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
#include "com_asakusafw_m3bp_mirror_jni_FlowGraphMirrorImpl.h"
#include "mirror.hpp"
#include "jniutil.hpp"

using namespace asakusafw::jni;

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_FlowGraphMirrorImpl
 * Method:    addVertex0
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_FlowGraphMirrorImpl_addVertex0
(JNIEnv *env, jclass, jlong _self, jstring _name) {
    try {
        auto* self = reinterpret_cast<FlowGraphMirror*>(_self);
        auto name = extract_string(env, _name);
        auto* vertex = self->vertex(name);
        return to_pointer(vertex);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_FlowGraphMirrorImpl
 * Method:    addEdge0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_FlowGraphMirrorImpl_addEdge0
(JNIEnv *env, jclass, jlong _self, jlong _upstream, jlong _downstream) {
    try {
        auto* self = reinterpret_cast<FlowGraphMirror*>(_self);
        auto* upstream = reinterpret_cast<OutputPortMirror*>(_upstream);
        auto* downstream = reinterpret_cast<InputPortMirror*>(_downstream);
        self->edge(upstream, downstream);
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}
