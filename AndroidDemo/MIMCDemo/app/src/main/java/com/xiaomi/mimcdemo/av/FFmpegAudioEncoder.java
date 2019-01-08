package com.xiaomi.mimcdemo.av;

import com.xiaomi.mimcdemo.listener.OnAudioEncodedListener;

public class FFmpegAudioEncoder implements Codec {
    private static final String TAG = "FFmpegAudioEncoder";
    private OnAudioEncodedListener onAudioEncodedListener;
    private long sequence = 0;


    public void setOnAudioEncodedListener(OnAudioEncodedListener onAudioEncodedListener) {
        this.onAudioEncodedListener = onAudioEncodedListener;
    }

    @Override
    public boolean start() {
        sequence = 0;
        int ret = startEncoder();
        if (ret != -1) return true;
        else return false;
    }

    @Override
    public void stop() {
        sequence = 0;
        stopEncoder();
    }

    @Override
    public boolean codec(byte[] data) {
        int ret = encode(data, data.length);
        if (ret != -1) return true;
        else return false;
    }

    public void onAudioEncoded(byte[] data) {
        onAudioEncodedListener.onAudioEncoded(data, ++sequence);
    }

    static {
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("codec");
    }

    private native int startEncoder();
    private native void stopEncoder();
    private native int encode(byte[] data, int len);
}
