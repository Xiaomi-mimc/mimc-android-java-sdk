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
    public static final int PING = 101;
    public static final int PONG = 102;
    //文本消息
    public static final int TEXT = 103;
    /**
     * 用户A将图片文件/语音文件/视频文件(非实时语音视频聊天)上传到文件存储服务器，获得一个URL
     * 用户A通过MIMC发送多媒体消息(content=URL)给用户B
     * 用户B接收多媒体消息(content=URL)，通过URL下载图片文件/语音文件/视频文件
     */
    public static final int PIC_FILE = 104;
    //已读消息，content为已读消息msgId
    public static final int TEXT_READ = 105;
    //撤回消息，content为撤回消息msgId
    public static final int TEXT_RECALL = 106;
    public static final int ADD_FRIEND_REQUEST = 107;
    //content为true or false,表示同意或拒绝
    public static final int ADD_FRIEND_RESPONSE = 108;

    public static final int VERSION = 0;

    public static final int DEFAULT_AUDIO_RECORD_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int DEFAULT_SAMPLE_RATE = 44100;    // 8000 11025 16000 22050 44100 48000
    public static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;     // CHANNEL_IN_MONO CHANNEL_IN_STEREO
    public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static final int DEFAULT_PLAY_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    public static final int DEFAULT_PLAY_STREAM_TYPE = AudioManager.STREAM_VOICE_CALL;      // AudioManager.STREAM_VOICE_CALL
    public static final int DEFAULT_PLAY_MODE = AudioTrack.MODE_STREAM;

    public static final int DEFAULT_CHANNEL_COUNT = 1;
    public static final int DEFAULT_BIT_RATE = 128 * 1000;  // 64000 96000 128000
    public static final int DEFAULT_MAX_BUFFER_SIZE = 512 * 1024;
}
