cmake_minimum_required(VERSION 2.8)

project(m3bpjni-test)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wextra")

file(GLOB NATIVE "comparator/*.cpp")

add_library(m3bpjni-test SHARED ${NATIVE})
