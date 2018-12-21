package com.xiaomi.mimcdemo.av;

/**
 * Created by houminjiang on 18-5-29.
 */

public interface Codec {
    boolean start();
    void stop();
    boolean codec(byte[] data);
}
