package com.xiaomi.mimcdemo.listener;

import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.proto.RtsData;

/**
 * Created by houminjiang on 18-5-24.
 */

public interface OnCallStateListener {
    void onLaunched(String fromAccount, String fromResource, long chatId, byte[] data);
    void onAnswered(long chatId, boolean accepted, String errMsg);
    void handleData(long chatId, RtsDataType dataType, byte[] data);
    void onClosed(long chatId, String errMsg);
}
