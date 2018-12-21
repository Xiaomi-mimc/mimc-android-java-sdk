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
        if (onAudioDecodedListener != null) {
            onAudioDecodedListener.onAudioDecoded(data);
        }
    }

    static {
        System.loadLibrary("avcodec-57");
        System.loadLibrary("avformat-57");
        System.loadLibrary("avutil-55");
        System.loadLibrary("swresample-2");
        System.loadLibrary("codec");
    }

    private native int startDecoder();
    private native void stopDecoder();
    private native int decode(byte[] data, int len);
}
