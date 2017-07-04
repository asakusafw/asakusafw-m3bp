cmake_minimum_required(VERSION 2.8)

project(testing-M3bpLauncher)

file(GLOB NATIVE "*.cpp")

add_library(testing-M3bpLauncher SHARED ${NATIVE})
set_target_properties(testing-M3bpLauncher PROPERTIES COMPILE_FLAGS "-std=c++11 -g -Wall")

if (WIN32)
    set_target_properties(testing-M3bpLauncher PROPERTIES PREFIX "")
endif(WIN32)
