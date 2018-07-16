package com.xiaomi.mimcdemo.bean;


/**
 * Created by muzi on 18-3-30.
 */

public class ChatMsg {
    private String fromAccount;
    private Msg msg;
    private Boolean isSingle;

    public ChatMsg() {}

    public ChatMsg(String fromAccount, Msg msg, Boolean isSingle) {
        this.fromAccount = fromAccount;
        this.msg = msg;
        this.isSingle = isSingle;
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
