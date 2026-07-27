# Native build for the on-device inference library (libainotebook_llama.so).
#
# This is invoked by Gradle ONLY when data/src/main/cpp/llama.cpp exists (the build.gradle.kts gates
# externalNativeBuild on that directory). Vendor llama.cpp there (git submodule) to enable it.

cmake_minimum_required(VERSION 3.22.1)
project("ainotebook_llama")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp build options tuned for a lean Android library build.
set(GGML_LLAMAFILE      OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP         OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL          OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER  OFF CACHE BOOL "" FORCE)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

add_library(ainotebook_llama SHARED ainotebook_llama.cpp)

find_library(log-lib log)

# Linking `llama` propagates its public include dirs, so `#include "llama.h"` resolves.
target_link_libraries(ainotebook_llama llama ${log-lib})
