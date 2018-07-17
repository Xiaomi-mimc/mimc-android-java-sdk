package com.xiaomi.mimcdemo.bean;

import com.alibaba.fastjson.annotation.JSONField;
import com.xiaomi.mimcdemo.common.MsgHelper;

/**
 * Created by muzi on 18-3-30.
 */

public class Msg {
    @JSONField(ordinal = 1)
    private int version;

    @JSONField(ordinal = 2)
    private String msgId;

    @JSONField(ordinal = 3)
    private int msgType;

    @JSONField(ordinal = 4)
    private long timestamp;

    @JSONField(ordinal = 5)
    private byte[] content;

    public Msg() {}

    public Msg(int version, String msgId, int msgType, long timestamp, byte[] content) {
        this.version = version;
        this.msgId = msgId;
        this.msgType = msgType;
        this.timestamp = timestamp;
        this.content = content;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getMsgId() {
        if (msgId == null) {
            msgId = MsgHelper.nextID();
        }
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
