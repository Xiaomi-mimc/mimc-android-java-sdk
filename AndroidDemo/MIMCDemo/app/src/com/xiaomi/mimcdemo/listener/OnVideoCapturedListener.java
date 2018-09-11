package com.xiaomi.mimcdemo.listener;

/**
 * Created by houminjiang on 18-5-28.
 */

public interface OnVideoCapturedListener {
    void onVideoCaptured(byte[] data, int width, int height);
}
