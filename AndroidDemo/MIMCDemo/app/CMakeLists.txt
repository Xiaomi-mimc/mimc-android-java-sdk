cmake_minimum_required(VERSION 3.4.1)


add_library( codec
             SHARED
             src/main/jni/com_xiaomi_mimcdemo_av_FFmpegAudioEncoder.c
             src/main/jni/com_xiaomi_mimcdemo_av_FFmpegAudioDecoder.c
             )

include_directories(libs/include)

add_library( avcodec
        SHARED
        IMPORTED)
set_target_properties( avcodec
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavcodec.so)

add_library( avformat
        SHARED
        IMPORTED)
set_target_properties( avformat
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavformat.so)

add_library( avutil
        SHARED
        IMPORTED)
set_target_properties( avutil
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavutil.so)

add_library( swresample
        SHARED
        IMPORTED)
set_target_properties( swresample
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libswresample.so)

find_library(
        log-lib
        log)

target_link_libraries(
        codec
        avcodec
        avformat
        avutil
        swresample
        ${log-lib})