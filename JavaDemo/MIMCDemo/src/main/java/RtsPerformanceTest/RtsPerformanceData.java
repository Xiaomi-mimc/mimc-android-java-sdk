package RtsPerformanceTest;

public class RtsPerformanceData {
    private byte[] dataByte;
    private long dataTime;
    private long loginTime1;
    private long loginTime2;
    private long diaCallTime;
    private long recvDataTime;

    public RtsPerformanceData(byte[] dataByte, long dataTime) {
        this.dataByte = dataByte;
        this.dataTime = dataTime;
    }

    public RtsPerformanceData(long loginTime1, long loginTime2, long diaCallTime, long recvDataTime) {
        this.loginTime1 = loginTime1;
        this.loginTime2 = loginTime2;
        this.diaCallTime = diaCallTime;
        this.recvDataTime = recvDataTime;
    }

    public byte[] getData() {
        return dataByte;
    }

    public long getDataTime() {
        return dataTime;
    }

    public long getLoginTime1() {
        return loginTime1;
    }

    public long getLoginTime2() {
        return loginTime2;
    }

    public long getDiaCallTime() {
        return diaCallTime;
    }

    public long getRecvDataTime() {
        return recvDataTime;
    }
}
