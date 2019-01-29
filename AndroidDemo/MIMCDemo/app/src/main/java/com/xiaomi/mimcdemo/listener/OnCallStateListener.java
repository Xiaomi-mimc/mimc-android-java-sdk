package com.xiaomi.mimcdemo.listener;

import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.proto.RtsData;

/**
 * Created by houminjiang on 18-5-24.
 */

public interface OnCallStateListener {
    void onLaunched(String fromAccount, String fromResource, long callId, byte[] data);
    void onAnswered(long callId, boolean accepted, String errMsg);
    void handleData(long callId, RtsDataType dataType, byte[] data);
    void onClosed(long callId, String errMsg);
}
