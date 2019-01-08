package com.xiaomi.mimcdemo.av;

import com.xiaomi.mimcdemo.listener.OnAudioDecodedListener;

public class FFmpegAudioDecoder implements Codec {
    private static final String TAG = "FFmpegAudioDecoder";
    private OnAudioDecodedListener onAudioDecodedListener;


    public void setOnAudioDecodedListener(OnAudioDecodedListener onAudioDecodedListener) {
        this.onAudioDecodedListener = onAudioDecodedListener;
    }

    @Override
    public boolean start() {
        int ret = startDecoder();
        if (ret != -1) return true;
        else return false;
    }

    @Override
    public void stop() {
        stopDecoder();
    }

    @Override
    public boolean codec(byte[] data) {
        int ret = decode(data, data.length);
        if (ret != -1) return true;
        else return false;
    }

    public void onAudioDecoded(byte[] data) {
        onAudioDecodedListener.onAudioDecoded(data);
    }

    static {
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("codec");
    }

    private native int startDecoder();
    private native void stopDecoder();
    private native int decode(byte[] data, int len);
}
