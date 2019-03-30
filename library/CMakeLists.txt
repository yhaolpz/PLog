
cmake_minimum_required(VERSION 3.4.1)

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

