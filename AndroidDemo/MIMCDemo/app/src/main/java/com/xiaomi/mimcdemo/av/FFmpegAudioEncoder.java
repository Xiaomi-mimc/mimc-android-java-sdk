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
        if (onAudioEncodedListener != null) {
            onAudioEncodedListener.onAudioEncoded(data, ++sequence);
        }
    }

    static {
        System.loadLibrary("avcodec-57");
        System.loadLibrary("avformat-57");
        System.loadLibrary("avutil-55");
        System.loadLibrary("swresample-2");
        System.loadLibrary("codec");
    }

    private native int startEncoder();
    private native void stopEncoder();
    private native int encode(byte[] data, int len);
}
