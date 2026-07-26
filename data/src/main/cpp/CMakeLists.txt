# Native build for the on-device inference library (libainotebook_llama.so).
#
# NOT wired into Gradle by default — enabling externalNativeBuild before vendoring llama.cpp would
# fail the build. To enable:
#   1. Vendor llama.cpp into data/src/main/cpp/llama.cpp (e.g. a git submodule).
#   2. Uncomment add_subdirectory(llama.cpp) and the `llama` link line below.
#   3. Uncomment the externalNativeBuild / ndk blocks in data/build.gradle.kts.
# See README.md in this directory.

cmake_minimum_required(VERSION 3.22.1)
project("ainotebook_llama")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build llama.cpp for the target ABI (CPU; GPU/Vulkan can be enabled per llama.cpp docs).
# add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

add_library(ainotebook_llama SHARED ainotebook_llama.cpp)

find_library(log-lib log)

target_link_libraries(ainotebook_llama
    # llama          # provided by add_subdirectory(llama.cpp)
    ${log-lib})
