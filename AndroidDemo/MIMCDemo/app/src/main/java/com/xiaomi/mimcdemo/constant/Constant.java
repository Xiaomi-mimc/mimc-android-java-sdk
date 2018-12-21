package com.xiaomi.mimcdemo.constant;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public final class Constant {
    /**
     * 检查用户在线
     * 用户A通过MIMC发送ping给用户B
     * 用户B接收到ping后，通过MIMC发送pong给用户A
     */
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    //文本消息
    public static final String TEXT = "TEXT";
    /**
     * 用户A将图片文件/语音文件/视频文件(非实时语音视频聊天)上传到文件存储服务器，获得一个URL
     * 用户A通过MIMC发送多媒体消息(content=URL)给用户B
     * 用户B接收多媒体消息(content=URL)，通过URL下载图片文件/语音文件/视频文件
     */
    public static final String PIC_FILE = "PIC_FILE";
    //已读消息，content为已读消息msgId
    public static final String TEXT_READ = "TEXT_READ";
    //撤回消息，content为撤回消息msgId
    public static final String TEXT_RECALL = "TEXT_RECALL";
    public static final String ADD_FRIEND_REQUEST = "ADD_FRIEND_REQUEST";
    //content为true or false,表示同意或拒绝
    public static final String ADD_FRIEND_RESPONSE = "ADD_FRIEND_RESPONSE";

    public static final int VERSION = 0;

    // MediaRecorder.AudioSource.MIC　该模式需要单独做降噪回声消除处理
    // MediaRecorder.AudioSource.VOICE_COMMUNICATION 该模式可以达到降噪回声消除效果，缺点声音小
    public static final int DEFAULT_AUDIO_RECORD_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100;    // 8000 11025 16000 22050 44100 48000
    public static final int DEFAULT_AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;     // CHANNEL_IN_MONO CHANNEL_IN_STEREO
    public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;     // ENCODING_PCM_8BIT

    public static final int DEFAULT_PLAY_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    public static final int DEFAULT_PLAY_STREAM_TYPE = AudioManager.STREAM_VOICE_CALL;      // AudioManager.STREAM_VOICE_CALL
    public static final int DEFAULT_PLAY_MODE = AudioTrack.MODE_STREAM;

    public static final int DEFAULT_CODEC_CHANNEL_COUNT = 2;                // 注意：按单声道数流量增大1倍多
    public static final int DEFAULT_ENCODER_BIT_RATE = 64 * 1000;           // 64000 96000 128000

    public static final int DEFAULT_VIDEO_FRAME_RATE = 30;  // 影响画面的流畅度
    public static final int DEFAULT_VIDEO_BIT_RATE = 256 * 1000;
}
