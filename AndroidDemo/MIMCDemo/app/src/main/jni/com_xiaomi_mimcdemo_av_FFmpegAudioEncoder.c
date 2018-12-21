//
// Created by houminjiang on 12/4/18.
//
#include "com_xiaomi_mimcdemo_av_FFmpegAudioEncoder.h"
#include <android/log.h>
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"


#define LOG_TAG "FFmpegAudioEncoder"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


AVCodec * avEncodeCodec = NULL;
AVCodecContext *avEncodeContext = NULL;
AVFrame *avEncodeFrame = NULL;
SwrContext *swrEncodeContext = NULL;
AVPacket *avEncodePacket = NULL;

int check_sample_fmt(const AVCodec *codec, enum AVSampleFormat sample_fmt) {
    const enum AVSampleFormat *p = codec->sample_fmts;
    while (*p != AV_SAMPLE_FMT_NONE) {
        LOGE("Encoder support sample format:%s", av_get_sample_fmt_name(*p));
        if (*p == sample_fmt)
            return 1;
        p++;
    }

    return 0;
}

JNIEXPORT jint JNICALL Java_com_xiaomi_mimcdemo_av_FFmpegAudioEncoder_startEncoder(JNIEnv *env, jobject obj) {
    // 注册编码器
    avcodec_register_all();
    // 查找编码器
    avEncodeCodec = avcodec_find_encoder(AV_CODEC_ID_AAC);
    if (!avEncodeCodec) {
        LOGE("Encoder not found.");
        return -1;
    }

    avEncodeContext = avcodec_alloc_context3(avEncodeCodec);
    if (!avEncodeContext) {
        LOGE("Could not allocate audio encode context.");
        return -1;
    }
    avEncodeContext->codec_id = AV_CODEC_ID_AAC;
    avEncodeContext->codec_type = AVMEDIA_TYPE_AUDIO;
    avEncodeContext->bit_rate = 32000;
    avEncodeContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
    if (!check_sample_fmt(avEncodeCodec, avEncodeContext->sample_fmt)) {
        LOGE("Encoder does not support sample format %s", av_get_sample_fmt_name(avEncodeContext->sample_fmt));
        avcodec_free_context(&avEncodeContext);
        return -1;
    }
    avEncodeContext->sample_rate = 44100;
    avEncodeContext->channel_layout = AV_CH_LAYOUT_MONO;
    avEncodeContext->channels = av_get_channel_layout_nb_channels(avEncodeContext->channel_layout);

    // 初始化重采样
    swrEncodeContext = swr_alloc_set_opts(NULL,
                                    av_get_default_channel_layout(avEncodeContext->channels),
                                    avEncodeContext->sample_fmt,
                                    avEncodeContext->sample_rate,
                                    av_get_default_channel_layout(avEncodeContext->channels),
                                    AV_SAMPLE_FMT_S16,
                                    avEncodeContext->sample_rate,
                                    0,
                                    NULL);
    if (!swrEncodeContext) {
        LOGE("Call swr_alloc_set_opts error.");
        avcodec_free_context(&avEncodeContext);
        return -1;
    }
    int ret = swr_init(swrEncodeContext);
    if (ret < 0) {
        LOGE("Call swr_init error.");
        swr_free(&swrEncodeContext);
        avcodec_free_context(&avEncodeContext);
        return -1;
    }

    avEncodePacket = av_packet_alloc();
    if (!avEncodePacket) {
        LOGE("Could not alloc encode packet.");
        swr_free(&swrEncodeContext);
        avcodec_free_context(&avEncodeContext);
        return -1;
    }
    av_init_packet(avEncodePacket);

    // 打开编码器
    if (avcodec_open2(avEncodeContext, avEncodeCodec, NULL) < 0) {
        LOGE("Could not open encoder.");
        av_packet_free(&avEncodePacket);
        swr_free(&swrEncodeContext);
        avcodec_free_context(&avEncodeContext);
        return -1;
    }

    // 打开编码器后分配
    avEncodeFrame = av_frame_alloc();
    if (!avEncodeFrame) {
        LOGE("Could not allocate audio frame.");
        av_packet_free(&avEncodePacket);
        swr_free(&swrEncodeContext);
        avcodec_close(avEncodeContext);
        avcodec_free_context(&avEncodeContext);
        return -1;
    }
    avEncodeFrame->nb_samples = avEncodeContext->frame_size;
    avEncodeFrame->format = avEncodeContext->sample_fmt;
    avEncodeFrame->channel_layout = avEncodeContext->channel_layout;

    ret = av_frame_get_buffer(avEncodeFrame, 0);
    if (ret < 0) {
        LOGE("Could not allocate audio data buffers.");
        av_frame_free(&avEncodeFrame);
        av_packet_free(&avEncodePacket);
        swr_free(&swrEncodeContext);
        avcodec_close(avEncodeContext);
        avcodec_free_context(&avEncodeContext);
    }

    return 0;
}

JNIEXPORT void JNICALL Java_com_xiaomi_mimcdemo_av_FFmpegAudioEncoder_stopEncoder(JNIEnv *env, jobject obj) {
    if(avEncodeFrame){
        av_frame_free(&avEncodeFrame);
    }
    if (avEncodePacket) {
        av_packet_free(&avEncodePacket);
    }
    if (swrEncodeContext) {
        swr_free(&swrEncodeContext);
    }
    if(avEncodeContext){
        avcodec_close(avEncodeContext);
        avcodec_free_context(&avEncodeContext);
    }
}

JNIEXPORT jint JNICALL Java_com_xiaomi_mimcdemo_av_FFmpegAudioEncoder_encode(JNIEnv *env, jobject obj, jbyteArray data, jint len) {
    jbyte *pData = (*env)->GetByteArrayElements(env, data, 0);
    if (!pData) {
        LOGE("Get pcm data error.");
        return -1;
    }

    if (!avEncodeContext || !avEncodeFrame) {
        LOGE("avEncodeContext or avEncodeFrame is null.");
        (*env)->ReleaseByteArrayElements(env, data, pData, 0);
        return -1;
    }

    int ret = av_frame_make_writable(avEncodeFrame);
    if (ret < 0) {
        LOGE("Frame is not writable.");
        (*env)->ReleaseByteArrayElements(env, data, pData, 0);
        return -1;
    }

    // 重采样
    ret = swr_convert(
            swrEncodeContext,
            avEncodeFrame->data,
            avEncodeFrame->nb_samples,
            &pData,
            avEncodeFrame->nb_samples);
    if (ret < 0) {
        LOGE("Call swr_convert error.");
        (*env)->ReleaseByteArrayElements(env, data, pData, 0);
        return -1;
    }

    avEncodePacket->data = NULL;
    avEncodePacket->size = 0;
    ret = avcodec_send_frame(avEncodeContext, avEncodeFrame);
    if (ret < 0) {
        LOGE("Error sending the frame to the encoder.");
        (*env)->ReleaseByteArrayElements(env, data, pData, 0);
        return -1;
    }
    while (ret >= 0) {
        ret = avcodec_receive_packet(avEncodeContext, avEncodePacket);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            (*env)->ReleaseByteArrayElements(env, data, pData, 0);
            return 0;
        } else if (ret < 0) {
            LOGE("Error during encoding. ret:%d", ret);
            (*env)->ReleaseByteArrayElements(env, data, pData, 0);
            return -1;
        }

        // 回调给外层
        jclass cls = (*env)->GetObjectClass(env, obj);
        if (!cls) {
            LOGE("Get cls is null.");
            (*env)->ReleaseByteArrayElements(env, data, pData, 0);
            return -1;
        }
        jmethodID mtd = (*env)->GetMethodID(env, cls, "onAudioEncoded", "([B)V");
        if (!mtd) {
            LOGE("Get mtd is null.");
            (*env)->ReleaseByteArrayElements(env, data, pData, 0);
            return -1;
        }

        jbyteArray encoded = (*env)->NewByteArray(env, avEncodePacket->size);
        (*env)->SetByteArrayRegion(env, encoded, 0, avEncodePacket->size, avEncodePacket->data);
        (*env)->CallVoidMethod(env, obj, mtd, encoded);
        (*env)->DeleteLocalRef(env, encoded);
        av_packet_unref(avEncodePacket);
    }

    return 0;
}