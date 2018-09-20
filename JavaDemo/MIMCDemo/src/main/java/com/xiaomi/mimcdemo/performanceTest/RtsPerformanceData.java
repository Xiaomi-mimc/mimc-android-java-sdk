package com.xiaomi.mimcdemo.performanceTest;

public class RtsPerformanceData {
    private byte[] dataByte;
    private long dataTime;

    public RtsPerformanceData(byte[] dataByte, long dataTime) {
        this.dataByte = dataByte;
        this.dataTime = dataTime;
    }

    public void setDataByte(byte[] dataByte) {
        this.dataByte = dataByte;
    }

    public void setDataTime(long dataTime) {
        this.dataTime = dataTime;
    }

    public byte[] getData() {
        return dataByte;
    }

    public long getTime() {
        return dataTime;
    }
}
