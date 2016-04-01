cmake_minimum_required(VERSION 2.8)

project(m3bpjni)

file(GLOB NATIVE "jni/*.cpp" "mirror/*.cpp" "adapter/*.cpp")

find_package(JNI)
message(STATUS "JNI: ${JNI_INCLUDE_DIRS}")

include_directories(${JNI_INCLUDE_DIRS})
include_directories(${JNI_HEADER_DIR})
include_directories("../include")
include_directories("${M3BP_INCLUDE_DIR}")

add_library(m3bpjni SHARED ${NATIVE})

if(UNIX)
    set(COMPILE_OPTIONS "-std=c++11 -g -Wall -fPIC -flto")
endif()

set_target_properties(m3bpjni PROPERTIES COMPILE_FLAGS ${COMPILE_OPTIONS})
set_target_properties(m3bpjni PROPERTIES INTERPROCEDURAL_OPTIMIZATION ON)

if (CUSTOM_LIBRARIES_DIR)
    message(STATUS "LIB: ${CUSTOM_LIBRARIES_DIR}")
    link_directories(${CUSTOM_LIBRARIES_DIR})
    set_target_properties(m3bpjni PROPERTIES LINK_FLAGS "-L${CUSTOM_LIBRARIES_DIR}")
    target_link_libraries(m3bpjni m3bp)
endif()
