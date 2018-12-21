cmake_minimum_required(VERSION 3.4.1)


add_library( codec
             SHARED
             src/main/jni/com_xiaomi_mimcdemo_av_FFmpegAudioEncoder.c
             src/main/jni/com_xiaomi_mimcdemo_av_FFmpegAudioDecoder.c
             )

include_directories(libs/include)

add_library( avcodec-57
             SHARED
             IMPORTED)
set_target_properties( avcodec-57
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavcodec-57.so)

add_library( avformat-57
             SHARED
             IMPORTED)
set_target_properties( avformat-57
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavformat-57.so)

add_library( avutil-55
             SHARED
             IMPORTED)
set_target_properties( avutil-55
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavutil-55.so)

add_library( swresample-2
             SHARED
             IMPORTED)
set_target_properties( swresample-2
                       PROPERTIES IMPORTED_LOCATION
                       ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libswresample-2.so)

find_library(
        log-lib
        log)

target_link_libraries(
        codec
        avcodec-57
        avformat-57
        avutil-55
        swresample-2
        ${log-lib})