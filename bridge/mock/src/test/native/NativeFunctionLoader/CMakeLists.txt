cmake_minimum_required(VERSION 2.8)

project(testing-NativeFunctionLoader)

file(GLOB NATIVE "*.cpp")

add_library(testing-NativeFunctionLoader SHARED ${NATIVE})
set_target_properties(testing-NativeFunctionLoader PROPERTIES COMPILE_FLAGS "-std=c++11 -g -Wall")

if (WIN32)
    set_target_properties(testing-NativeFunctionLoader PROPERTIES PREFIX "")
endif(WIN32)
