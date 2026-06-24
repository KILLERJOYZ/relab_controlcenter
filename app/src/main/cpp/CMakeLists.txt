cmake_minimum_required(VERSION 3.22.1)
project("rlcc_bench" C)

# Compile with performance optimisations (-O3) and strip debug symbols for APK size compliance
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fvisibility=hidden -ffunction-sections -fdata-sections")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections -Wl,--strip-all")

add_library(
    rlcc_bench
    SHARED
    benchmark_native.c
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    rlcc_bench
    ${log-lib}
    ${android-lib}
)
