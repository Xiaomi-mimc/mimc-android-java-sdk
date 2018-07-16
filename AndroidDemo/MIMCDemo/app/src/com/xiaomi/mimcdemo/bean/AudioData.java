package com.xiaomi.mimcdemo.bean;

import java.io.Serializable;

/**
 * Created by houminjiang on 18-7-3.
 */

public class AudioData implements Serializable {
    private static final long serialVersionUID = 3999951906575286192L;
    private int version;
    private long index;
    private byte[] data;

    public AudioData(byte[] data) {
        this(0, data);
    }

    public AudioData(long index, byte[] data) {
        this(0, index, data);
    }

    public AudioData(int version, long index, byte[] data) {
        this.version = version;
        this.index = index;
        this.data = data;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getIndex() {
        return index;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
