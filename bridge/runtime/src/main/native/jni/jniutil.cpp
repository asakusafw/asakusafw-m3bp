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
#include "jniutil.hpp"

static thread_local jmethodID _object_to_string = nullptr;

jlong to_pointer(void *p) {
    return (jlong) p;
}

jobject to_java_buffer(JNIEnv *env, const std::tuple<const void *, size_t> &range) {
    if (std::get<0>(range) == nullptr) {
        return nullptr;
    }
    jobject result = env->NewDirectByteBuffer(const_cast<void *>(std::get<0>(range)), std::get<1>(range));
    check_java_exception(env);
    return result;
}

jclass find_class(JNIEnv *env, const char *name) {
    jclass clazz = env->FindClass(name);
    check_java_exception(env);
    return clazz;
}

jmethodID find_method(JNIEnv *env, jclass clazz, const char *name, const char *signature) {
    jmethodID id = env->GetMethodID(clazz, name, signature);
    check_java_exception(env);
    return id;
}

void check_java_exception(JNIEnv *env) {
    jthrowable object = env->ExceptionOccurred();
    if (object) {
        env->ExceptionClear();
        throw JavaException(object);
    }
}

void handle_native_exception(JNIEnv *env, std::exception &e) {
    const char *what = e.what();
    jclass clazz = find_class(env, "com/asakusafw/m3bp/mirror/jni/NativeException");
    env->ThrowNew(clazz, what ? what : "(unknown reason)");
}

jobject new_global_ref(JNIEnv *env, jobject object) {
    jobject global = env->NewGlobalRef(object);
    check_java_exception(env);
    env->DeleteLocalRef(object);
    check_java_exception(env);
    return global;
}

void delete_global_ref(JNIEnv *env, jobject object) {
    env->DeleteGlobalRef(object);
    check_java_exception(env);
}

std::string java_to_string(JNIEnv *env, jobject object) {
    if (!object) {
        return std::string("null");
    }
    LocalFrame(env, 4);
    if (!_object_to_string) {
        jclass clazz = env->FindClass("java/lang/Object");
        check_java_exception(env);
        _object_to_string = env->GetMethodID(clazz, "toString", "()Ljava/lang/String;");
        check_java_exception(env);
    }
    jstring string = (jstring) env->CallObjectMethod(object, _object_to_string);
    check_java_exception(env);
    if (!string) {
        return std::string("null");
    }
    const char *contents = env->GetStringUTFChars(string, 0);
    std::string results(contents);
    env->ReleaseStringUTFChars(string, contents);
    return results;
}
