//
// Created by houminjiang on 12/6/18.
//
#include "com_xiaomi_mimcdemo_av_FFmpegAudioDecoder.h"
#include <android/log.h>
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"


#define LOG_TAG "FFmpegAudioDecoder"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AVCodec * avDecode = NULL;
AVCodecContext *avDecodeContext = NULL;
SwrContext *swrDecoeContext = NULL;
AVFrame *avDecodeFrame = NULL;
AVPacket *avDecodePacket = NULL;

JNIEXPORT jint JNICALL Java_com_xiaomi_mimcdemo_av_FFmpegAudioDecoder_startDecoder(JNIEnv *env, jobject obj) {
    // 注册解码器
    avcodec_register_all();
    // 查找解码器
    avDecode = avcodec_find_decoder(AV_CODEC_ID_AAC);
    if (!avDecode) {
        LOGE("Decoder not found.");
        return -1;
    }

    avDecodeContext = avcodec_alloc_context3(avDecode);
    if (!avDecodeContext) {
        LOGE("Could not allocate audio decode context.");
        return -1;
    }
    avDecodeContext->codec_id = AV_CODEC_ID_AAC;
    avDecodeContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
    avDecodeContext->codec_type = AVMEDIA_TYPE_AUDIO;
    avDecodeContext->bit_rate = 32000;
    avDecodeContext->sample_rate = 44100;
    avDecodeContext->channel_layout = AV_CH_LAYOUT_MONO;
    avDecodeContext->channels = av_get_channel_layout_nb_channels(avDecodeContext->channel_layout);

    // 初始化重采样
    swrDecoeContext = swr_alloc_set_opts(
            NULL,
            av_get_default_channel_layout(avDecodeContext->channels),
            AV_SAMPLE_FMT_S16,
            avDecodeContext->sample_rate,
            av_get_default_channel_layout(avDecodeContext->channels),
            avDecodeContext->sample_fmt,
            avDecodeContext->sample_rate,
            0,
            NULL);
    if (!swrDecoeContext) {
        LOGE("Call swr_alloc_set_opts error.");
        avcodec_free_context(&avDecodeContext);
        return -1;
    }
    int ret = swr_init(swrDecoeContext);
    if (ret < 0) {
        LOGE("Call swr_init error.");
        swr_free(&swrDecoeContext);
        avcodec_free_context(&avDecodeContext);
        return -1;
    }

    avDecodePacket = av_packet_alloc();
    if (!avDecodePacket) {
        LOGE("Could not alloc packet.");
        swr_free(&swrDecoeContext);
        avcodec_free_context(&avDecodeContext);
        return -1;
    }
    av_init_packet(avDecodePacket);

    avDecodeFrame = av_frame_alloc();
    if (!avDecodeFrame) {
        LOGE("Could not alloc decoded frame.");
        av_packet_free(&avDecodePacket);
        swr_free(&swrDecoeContext);
        avcodec_free_context(&avDecodeContext);
        return -1;
    }

    // 打开解码器
    if (avcodec_open2(avDecodeContext, avDecode, NULL) < 0) {
        LOGE("Could not open decoder.");
        av_frame_free(&avDecodeFrame);
        av_packet_free(&avDecodePacket);
        swr_free(&swrDecoeContext);
        avcodec_free_context(&avDecodeContext);
        return -1;
    }

    return 0;
}

JNIEXPORT void JNICALL Java_com_xiaomi_mimcdemo_av_FFmpegAudioDecoder_stopDecoder(JNIEnv *env, jobject obj) {
    if (avDecodeFrame) {
        av_frame_free(&avDecodeFrame);
    }
    if (avDecodePacket) {
        av_packet_free(&avDecodePacket);
    }
    if (swrDecoeContext) {
        swr_free(&swrDecoeContext);
    }
    if(avDecodeContext){
        avcodec_close(avDecodeContext);
        avcodec_free_context(&avDecodeContext);
    }
}

JNIEXPORT jint JNICALL Java_com_xiaomi_mimcdemo_av_FFmpegAudioDecoder_decode(JNIEnv *env, jobject obj, jbyteArray data, jint len) {
    jbyte *aacData = (*env)->GetByteArrayElements(env, data, 0);
    if (!aacData) {
        LOGE("Get aac data error.");
        return -1;
    }

    if (!avDecodeContext || !avDecodePacket) {
        LOGE("avDecodeContext or avDecodePacket is null.");
        (*env)->ReleaseByteArrayElements(env, data, aacData, 0);
        return -1;
    }

    avDecodePacket->data = (uint8_t *)aacData;
    avDecodePacket->size = (int)len;

    if (avDecodePacket->size > 0) {
        int ret = avcodec_send_packet(avDecodeContext, avDecodePacket);
        if (ret < 0) {
            LOGE("Error submitting the packet to the decoder, ret:%d", ret);
            (*env)->ReleaseByteArrayElements(env, data, aacData, 0);
            return -1;
        }

        while (ret >= 0) {
            ret = avcodec_receive_frame(avDecodeContext, avDecodeFrame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                (*env)->ReleaseByteArrayElements(env, data, aacData, 0);
                return 0;
            } else if (ret < 0) {
                LOGE("Error during decoding. ret:%d\", ret");
                (*env)->ReleaseByteArrayElements(env, data, aacData, 0);
                return -1;
            }

            int outLineSize;
            int outBufferSize = av_samples_get_buffer_size(
                    &outLineSize,
                    avDecodeContext->channels,
                    avDecodeContext->frame_size,
                    AV_SAMPLE_FMT_S16,
                    1);
            uint8_t *outBuffer = (uint8_t *)av_malloc(outBufferSize);
            // 重采样
            ret = swr_convert(
                    swrDecoeContext,
                    &outBuffer,
                    outLineSize,
                    (const uint8_t **)avDecodeFrame->data,
                    avDecodeFrame->nb_samples);
            if (ret < 0) {
                LOGE("Call swr_convert error.");
                (*env)->ReleaseByteArrayElements(env, data, aacData, 0);
                av_free(outBuffer);
                return -1;
            }

            // 回调给外层
            jclass cls = (*env)->GetObjectClass(env, obj);
            if (!cls) {
                LOGE("Get cls is null.");
                (*env)->ReleaseByteArrayElements(env, data, aacData, 0);
                av_free(outBuffer);
                return -1;
            }
            jmethodID mtd = (*env)->GetMethodID(env, cls, "onAudioDecoded", "([B)V");
            if (!mtd) {
                LOGE("Get mtd is null.");
                (*env)->ReleaseByteArrayElements(env, data, aacData, 0);
                av_free(outBuffer);
                return -1;
            }

            jbyteArray decoded = (*env)->NewByteArray(env, outBufferSize);
            (*env)->SetByteArrayRegion(env, decoded, 0, outBufferSize, outBuffer);
            (*env)->CallVoidMethod(env, obj, mtd, decoded);
            (*env)->DeleteLocalRef(env, decoded);
            av_free(outBuffer);
            av_frame_unref(avDecodeFrame);
        }
    }

    return 0;
}
