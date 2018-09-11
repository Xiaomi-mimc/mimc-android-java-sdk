package com.xiaomi.mimcdemo.bean;

import java.io.Serializable;

/**
 * Created by houminjiang on 18-7-3.
 */

public class VideoData implements Serializable {
    private static final long serialVersionUID = 3999951906575286192L;
    private int version;
    private long sequence;
    private byte[] data;
    private int width;
    private int height;

    public VideoData(byte[] data, int width, int height) {
        this(0, data, width, height);
    }

    public VideoData(long sequence, byte[] data, int width, int height) {
        this(0, sequence, data, width, height);
    }

    public VideoData(int version, long sequence, byte[] data, int width, int height) {
        this.version = version;
        this.sequence = sequence;
        this.data = data;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getSequence() {
        return sequence;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }
}
