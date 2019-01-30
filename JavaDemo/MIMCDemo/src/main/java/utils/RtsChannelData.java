package utils;

import com.xiaomi.mimc.data.ChannelUser;
import com.xiaomi.mimc.data.RtsDataType;

import java.util.List;

public class RtsChannelData {
    private long callId;
    private long identity;
    private int groupId;
    private boolean success;
    private byte[] data;
    private byte[] extra;
    private String account;
    private String resource;
    private String callKey;
    private String desc;
    private Object context;
    private RtsDataType dataType;
    private List<ChannelUser> members;

    public RtsChannelData(long identity, long callId, String callKey, boolean success, String desc, byte[] extra) {
        this.identity = identity;
        this.callId = callId;
        this.callKey = callKey;
        this.success = success;
        this.desc = desc;
        this.extra = extra;
    }

    public RtsChannelData(long callId, String account, String resource, boolean success, String desc, byte[] extra, List<ChannelUser> members) {
        this.callId = callId;
        this.account = account;
        this.resource = resource;
        this.success = success;
        this.desc = desc;
        this.extra = extra;
        this.members = members;
    }

    public RtsChannelData(long callId, String account, String resource, boolean success, String desc) {
        this.callId = callId;
        this.account = account;
        this.resource = resource;
        this.success = success;
        this.desc = desc;
    }

    public RtsChannelData(long callId, String account, String resource) {
        this.callId = callId;
        this.account = account;
        this.resource = resource;
    }

    public RtsChannelData(long callId, String account, String resource, byte[] data, RtsDataType dataType) {
        this.callId = callId;
        this.account = account;
        this.resource = resource;
        this.data = data;
        this.dataType = dataType;
    }

    public RtsChannelData(long callId, int groupId, Object context) {
        this.callId = callId;
        this.groupId = groupId;
        this.context = context;
    }

    public long getCallId() {
        return callId;
    }

    public long getIdentity() {
        return identity;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getExtra() {
        return extra;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<ChannelUser> getMembers() {
        return members;
    }

    public String getAccount() {
        return account;
    }

    public String getResource() {
        return resource;
    }

    public String getCallKey() {
        return callKey;
    }

    public String getDesc() {
        return desc;
    }

    public RtsDataType getDataType() {
        return dataType;
    }

    public int getGroupId() {
        return groupId;
    }

    public Object getContext() {
        return context;
    }
}
