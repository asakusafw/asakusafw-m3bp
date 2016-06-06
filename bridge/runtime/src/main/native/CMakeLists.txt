cmake_minimum_required(VERSION 2.8)

project(m3bpjni)

find_package(JNI)

include_directories(${JNI_INCLUDE_DIRS})
include_directories(${JNI_HEADER_DIR})
include_directories("../include")
include_directories("${M3BP_INCLUDE_DIR}")
link_directories(${CUSTOM_LIBRARIES_DIR})

file(GLOB NATIVE "jni/*.cpp" "mirror/*.cpp" "adapter/*.cpp")

if (APPLE)
    MESSAGE(STATUS "enabling MacOSX workaround: -DM3BP_NO_THREAD_LOCAL")
    add_definitions(-DM3BP_NO_THREAD_LOCAL)
endif()

add_library(m3bpjni SHARED ${NATIVE})
set_target_properties(m3bpjni PROPERTIES COMPILE_FLAGS "-std=c++11 -Wall")
set_target_properties(m3bpjni PROPERTIES INTERPROCEDURAL_OPTIMIZATION ON)
target_link_libraries(m3bpjni m3bp)
