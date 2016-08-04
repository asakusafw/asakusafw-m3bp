cmake_minimum_required(VERSION 2.8)

project(testing-Launcher)

file(GLOB NATIVE "*.cpp")

add_library(testing-Launcher SHARED ${NATIVE})
set_target_properties(testing-Launcher PROPERTIES COMPILE_FLAGS "-std=c++11 -g -Wall")

if (WIN32)
    set_target_properties(testing-Launcher PROPERTIES PREFIX "")
endif(WIN32)
