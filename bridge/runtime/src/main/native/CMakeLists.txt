cmake_minimum_required(VERSION 2.8)

if (POLICY CMP0069)
  cmake_policy(SET CMP0069 OLD)
endif()

project(m3bpjni CXX)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wextra")

find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS})
include_directories(${JNI_HEADER_DIR})
include_directories("../include")
include_directories("${M3BP_INCLUDE_DIR}")
link_directories(${CUSTOM_LIBRARIES_DIR})

file(GLOB NATIVE "jni/*.cpp" "mirror/*.cpp" "adapter/*.cpp")

add_library(m3bpjni SHARED ${NATIVE})
set_target_properties(m3bpjni PROPERTIES INTERPROCEDURAL_OPTIMIZATION ON)
target_link_libraries(m3bpjni m3bp)
