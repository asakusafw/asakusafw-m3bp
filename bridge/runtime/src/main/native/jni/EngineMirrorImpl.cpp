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
#include "com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl.h"
#include "mirror.hpp"
#include "jniutil.hpp"

#include <string>
#include <dlfcn.h>
#include <iostream>

#include <m3bp/m3bp.hpp>

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl
 * Method:    initialize0
 * Signature: (Ljava/lang/String;)J
 */
jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl_initialize0
(JNIEnv *env, jobject _this, jstring _library) {
    try {
        std::string library_name;
        if (_library) {
            const char *path = env->GetStringUTFChars(_library, NULL);
            library_name = std::string(path);
            env->ReleaseStringUTFChars(_library, path);
        }
        jobject mirror = new_global_ref(env, _this);
        jclass clazz = find_class(env, "com/asakusafw/m3bp/mirror/jni/EngineMirrorImpl");

        EngineMirror *self = new EngineMirror(
            mirror, library_name,
            find_method(env, clazz, "doThreadInitialize", "()V"),
            find_method(env, clazz, "doThreadFinalize", "()V"),
            find_method(env, clazz, "doGlobalInitialize", "(JJ)V"),
            find_method(env, clazz, "doGlobalFinalize", "(JJ)V"),
            find_method(env, clazz, "doLocalInitialize", "(JJ)V"),
            find_method(env, clazz, "doLocalFinalize", "(JJ)V"),
            find_method(env, clazz, "doRun", "(JJ)V"),
            find_method(env, clazz, "doTaskCount", "(J)I"),
            find_method(env, clazz, "doMaxConcurrency", "(J)I"));
        return to_pointer(self);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl
 * Method:    getConfiguration0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl_getConfiguration0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        EngineMirror *self = (EngineMirror *) _self;
        ConfigurationMirror *conf = self->configuration();
        return to_pointer(conf);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl
 * Method:    getGraph0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl_getGraph0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        EngineMirror *self = (EngineMirror *) _self;
        FlowGraphMirror *graph = self->graph();
        return to_pointer(graph);
    } catch (JavaException &e) {
        e.rethrow(env);
        return 0;
    } catch (std::exception &e) {
        handle_native_exception(env, e);
        return 0;
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl
 * Method:    run0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl_run0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        EngineMirror *self = (EngineMirror *) _self;
        self->run();
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl_close0
(JNIEnv *env, jclass clazz, jlong _self) {
    try {
        EngineMirror *self = (EngineMirror *) _self;
        self->cleanup(env);
        delete_global_ref(env, self->mirror());
        delete self;
    } catch (JavaException &e) {
        e.rethrow(env);
    } catch (std::exception &e) {
        handle_native_exception(env, e);
    }
}

/*
 * Class:     com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl
 * Method:    initializeNativeLogger0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_asakusafw_m3bp_mirror_jni_EngineMirrorImpl_initializeNativeLogger0
(JNIEnv *env, jclass clazz, jint _level) {
    m3bp::LogLevel level = static_cast<m3bp::LogLevel>(_level);
    m3bp::Logger::add_destination_stream(std::clog, level);
}
