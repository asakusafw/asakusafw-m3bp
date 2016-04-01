cmake_minimum_required(VERSION 2.8)

project(test)

file(GLOB NATIVE "comparator/*.cpp")

add_library(test SHARED ${NATIVE})
set_target_properties(test PROPERTIES COMPILE_FLAGS "-std=c++11 -g -Wall")
