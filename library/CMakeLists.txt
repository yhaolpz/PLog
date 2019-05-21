
cmake_minimum_required(VERSION 3.4.1)

if(ANDROID_ABI OR ANDROID_PLATFORM)
    # this should before "project(xxx)", and must define "project(xxx)"
    if("${ANDROID_ABI}" STREQUAL "armeabi-v7a")
        set(ANDROID_STL "gnustl_shared")
        set(ANDROID_TOOLCHAIN "gcc")
    elseif ("${ANDROID_ABI}" STREQUAL "arm64-v8a")
        set(ANDROID_STL "c++_shared")
        set(ANDROID_TOOLCHAIN "clang")
    else()
        message(FATAL_ERROR "${ANDROID_ABI} is no support")
    endif()
endif()

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})

add_library(plog-lib
        SHARED
        src/main/cpp/plog-lib.cpp
        src/main/cpp/LogWriter.cpp
        src/main/cpp/ErrInfo.cpp
        src/main/cpp/JNIHelp.cpp
        )

find_library(log-lib
        log)

target_link_libraries(plog-lib
        ${log-lib})

