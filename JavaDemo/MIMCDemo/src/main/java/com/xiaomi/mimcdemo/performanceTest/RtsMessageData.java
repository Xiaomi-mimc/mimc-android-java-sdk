package com.xiaomi.mimcdemo.performanceTest;

import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.proto.RtsData;

import java.util.Arrays;

/**
 * Created by malijing on 18-3-23.
 */
public class RtsMessageData {
    private String fromAccount;
    private String fromResource;
    private String extramsg;
    private long chatId;
    private boolean accepted;
    private byte[] appContent;
    private byte[] recvData;
    private RtsDataType dataType;
    private RtsData.CHANNEL_TYPE channelType;

    public RtsMessageData(String fromAccount, String fromResource, long chatId, byte[] appContent) {
        this.fromAccount = fromAccount;
        this.fromResource = fromResource;
        this.chatId = chatId;
        this.appContent = appContent;
    }

    public RtsMessageData(long chatId, boolean accepted, String extramsg) {
        this.chatId = chatId;
        this.extramsg = extramsg;
        this.accepted = accepted;
    }

    public RtsMessageData(long chatId, String extramsg) {
        this.chatId = chatId;
        this.extramsg = extramsg;
    }

    public RtsMessageData(long chatId, byte[] recvData, RtsDataType dataType, RtsData.CHANNEL_TYPE channelType) {
        this.chatId = chatId;
        this.recvData = recvData;
        this.dataType = dataType;
        this.channelType = channelType;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getFromResource() {
        return fromResource;
    }

    public long getChatId() {
        return chatId;
    }

    public byte[] getAppContent() {
        return appContent;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getExtramsg() {
        return extramsg;
    }

    public byte[] getRecvData() {
        return recvData;
    }

    public RtsDataType getDataType() {
        return dataType;
    }

    public RtsData.CHANNEL_TYPE getChannelType() {
        return channelType;
    }

    @Override
    public String toString() {
        return "RtsMessageData{" +
                "fromAccount='" + fromAccount + '\'' +
                ", fromResource='" + fromResource + '\'' +
                ", chatId=" + chatId +
                ", data=" + Arrays.toString(appContent) +
                ", accepted=" + accepted +
                ", extramsg='" + extramsg + '\'' +
                '}';
    }
}
