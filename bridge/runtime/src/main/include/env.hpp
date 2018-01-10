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
#ifndef ENV_HPP
#define ENV_HPP

#include <jni.h>
#include <stdexcept>

JNIEnv *java_env();
JNIEnv *java_attach();
void java_detach();

class BridgeError : public std::runtime_error {
public:
    BridgeError(const char *message) : std::runtime_error(message) {}
    BridgeError(const std::string &message) : std::runtime_error(message) {}
};
 
#endif //ENV_HPP
