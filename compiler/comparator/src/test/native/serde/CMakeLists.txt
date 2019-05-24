cmake_minimum_required(VERSION 2.8)

project(test-serde)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wextra")

file(GLOB NATIVE "*.cpp")
file(GLOB CORE "../../../main/resources/com/asakusafw/m3bp/compiler/comparator/native/src/*.cpp")

include_directories("../../../main/resources/com/asakusafw/m3bp/compiler/comparator/native/include")

add_library(test-serde SHARED ${NATIVE} ${CORE})

if (WIN32)
    set_target_properties(test-serde PROPERTIES PREFIX "")
endif(WIN32)
