cmake_minimum_required(VERSION 2.8)

project(test-serde)

file(GLOB NATIVE "*.cpp")
file(GLOB CORE "../../../main/resources/com/asakusafw/m3bp/compiler/comparator/native/src/*.cpp")

include_directories("../../../main/resources/com/asakusafw/m3bp/compiler/comparator/native/include")

add_library(test-serde SHARED ${NATIVE} ${CORE})
set_target_properties(test-serde PROPERTIES COMPILE_FLAGS "-std=c++11 -Wall")

if (WIN32)
    set_target_properties(test-serde PROPERTIES PREFIX "")
endif(WIN32)
