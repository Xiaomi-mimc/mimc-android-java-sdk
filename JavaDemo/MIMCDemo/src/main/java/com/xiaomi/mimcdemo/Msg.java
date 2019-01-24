package com.xiaomi.mimcdemo;

public class Msg {
    private int version;

    private String msgId;

    private long timestamp;

    private byte[] content;

    public Msg() {}

    public Msg(int version, String msgId, long timestamp, byte[] content) {
        this.version = version;
        this.msgId = msgId;
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
