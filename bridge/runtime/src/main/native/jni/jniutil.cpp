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
#include "jniutil.hpp"
#include "env.hpp"
#include <mutex>

namespace asakusafw {
namespace jni {

jlong to_pointer(void const* p) noexcept {
    return reinterpret_cast<jlong>(p);
}

std::string extract_string(JNIEnv* env, jstring string) {
    if (string) {
        auto* contents = env->GetStringUTFChars(string, 0);
        std::string result { contents };
        env->ReleaseStringUTFChars(string, contents);
        return result;
    }
    return {};
}

jobject to_java_buffer(JNIEnv* env, const std::tuple<void const*, std::size_t>& range) {
    if (std::get<0>(range) == nullptr) {
        return nullptr;
    }
    jobject result = env->NewDirectByteBuffer(const_cast<void *>(std::get<0>(range)), std::get<1>(range));
    check_java_exception(env);
    return result;
}

jclass find_class(JNIEnv* env, char const* name) {
    jclass clazz = env->FindClass(name);
    check_java_exception(env);
    return clazz;
}

jmethodID find_method(JNIEnv* env, jclass clazz, char const* name, char const* signature) {
    jmethodID id = env->GetMethodID(clazz, name, signature);
    check_java_exception(env);
    return id;
}

void check_java_exception(JNIEnv* env) {
    jthrowable object = env->ExceptionOccurred();
    if (object) {
        env->ExceptionClear();
        throw JavaException(object);
    }
}

static std::mutex s_global_exception_mutex;

void check_java_exception(JNIEnv* env, std::vector<jobject> &global_refs) {
    jthrowable object = env->ExceptionOccurred();
    if (object) {
        env->ExceptionClear();
        jthrowable global_object = static_cast<jthrowable>(env->NewGlobalRef(object));
        if (!global_object) {
            throw std::runtime_error("failed to create global reference of exception");
        }
        {
            std::lock_guard<decltype(s_global_exception_mutex)> lock(s_global_exception_mutex);
            global_refs.emplace_back(global_object);
        }
        throw JavaException(global_object);
    }
}

void handle_native_exception(JNIEnv* env, std::exception const& e) {
    auto *what = e.what();
    jclass clazz = find_class(env, "com/asakusafw/m3bp/mirror/jni/NativeException");
    env->ThrowNew(clazz, what ? what : "(unknown reason)");
}

jobject new_global_ref(JNIEnv* env, jobject object) {
    jobject global_object = env->NewGlobalRef(object);
    if (!global_object) {
        throw std::runtime_error("failed to create global reference");
    }
    env->DeleteLocalRef(object);
    check_java_exception(env);
    return global_object;
}

void delete_global_ref(JNIEnv* env, jobject object) {
    env->DeleteGlobalRef(object);
    check_java_exception(env);
}

LocalFrame::LocalFrame(JNIEnv* env, jint capacity) :
        m_env(env),
        m_temporary(false) {
    m_env->PushLocalFrame(capacity);
    check_java_exception(m_env);
}

LocalFrame::LocalFrame(jint capacity) {
    JNIEnv* env = java_env();
    if (env) {
        m_env = env;
        m_temporary = false;
    } else {
        m_env = java_attach();
        m_temporary = true;
    }
    m_env->PushLocalFrame(capacity);
    check_java_exception(m_env);
}

LocalFrame::~LocalFrame() {
    m_env->PopLocalFrame(nullptr);
    if (m_temporary) {
        java_detach();
    }
}

}  // namespace jni
}  // namespace asakusafw
