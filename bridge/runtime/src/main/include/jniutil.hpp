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
#ifndef JNIUTIL_HPP
#define JNIUTIL_HPP

#include <jni.h>
#include <string>
#include <tuple>
#include <vector>
#include <exception>

namespace asakusafw {
namespace jni {

jlong to_pointer(void const*) noexcept;
std::string extract_string(JNIEnv* env, jstring string);
jobject to_java_buffer(JNIEnv* env, const std::tuple<void const*, std::size_t> &range);
jclass find_class(JNIEnv* env, char const* name);
jmethodID find_method(JNIEnv* env, jclass clazz, char const* name, char const* signature);
void check_java_exception(JNIEnv* env);
void check_java_exception(JNIEnv* env, std::vector<jobject> &global_refs);
void handle_native_exception(JNIEnv* env, std::exception &e);
jobject new_global_ref(JNIEnv* env, jobject object);
void delete_global_ref(JNIEnv* env, jobject object);

class LocalFrame {
private:
    JNIEnv *m_env;
    bool m_temporary;
public:
    LocalFrame(jint capacity);
    LocalFrame(JNIEnv* env, jint capacity);
    ~LocalFrame();
    inline JNIEnv* env() const noexcept {
        return m_env;
    }
};

class JavaException : public std::exception {
private:
    jthrowable m_throwable;
    std::string m_what;
public:
    JavaException(jobject throwable) :
            m_throwable((jthrowable) throwable),
            m_what("(java exception)") {
    }
    jthrowable throwable() noexcept {
        return m_throwable;
    }
    char const* what() const noexcept override {
        return m_what.data();
    }
    void rethrow(JNIEnv* env) {
        env->Throw(m_throwable);
    }
};

}  // namespace jni
}  // namespace asakusafw

#endif // JNIUTIL_HPP
