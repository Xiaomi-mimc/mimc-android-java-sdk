package com.xiaomi.mimcdemo.bean;


/**
 * Created by muzi on 18-3-30.
 */

public class ChatMsg {
    private String bizType;
    private String fromAccount;
    private Msg msg;
    private Boolean isSingle;

    public ChatMsg() {}

    public ChatMsg(String bizType, String fromAccount, Msg msg, Boolean isSingle) {
        this.bizType = bizType;
        this.fromAccount = fromAccount;
        this.msg = msg;
        this.isSingle = isSingle;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public Msg getMsg() {
        return msg;
    }

    public void setMsg(Msg msg) {
        this.msg = msg;
    }

    public Boolean getSingle() {
        return isSingle;
    }

    public void setSingle(Boolean single) {
        isSingle = single;
    }
}
