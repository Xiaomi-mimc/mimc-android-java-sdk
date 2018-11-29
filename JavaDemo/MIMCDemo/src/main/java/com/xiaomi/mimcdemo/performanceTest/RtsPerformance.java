package com.xiaomi.mimcdemo.performanceTest;

import com.xiaomi.mimc.MIMCOnlineStatusListener;
import com.xiaomi.mimc.MIMCUser;
import com.xiaomi.mimc.common.MIMCConstant;
import com.xiaomi.mimc.data.RtsChannelType;
import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.proto.RtsData;
import com.xiaomi.mimc.proto.RtsSignal;
import com.xiaomi.mimcdemo.performanceTest.utils.*;
import com.xiaomi.msg.data.XMDPacket;
import org.apache.commons.lang.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RtsPerformance {
    private static final Logger logger = LoggerFactory.getLogger(RtsPerformance.class);

    public static final String appId = "2882303761517613988";
    public static final String appKey = "5361761377988";
    public static final String appSecurity = "2SZbrJOAL1xHRKb7L9AiRQ==";

    public static final String urlForToken = "https://mimc.chat.xiaomi.net/api/account/token";
    private final String cachePath = "cacheFileRtsPerformance";

    private MIMCUser rtsUser1;
    private MIMCUser rtsUser2;

    private final String appAccount1 = "prod_rts_performance_account1_1";
    private final String appAccount2 = "prod_rts_performance_account2_2";

    private MIMCCaseMessageHandler msgHandler1 = new MIMCCaseMessageHandler();
    private MIMCCaseMessageHandler msgHandler2 = new MIMCCaseMessageHandler();

    private RtsPerformanceHandler callEventHandler1 = new RtsPerformanceHandler();
    private RtsPerformanceHandler callEventHandler2 = new RtsPerformanceHandler();

    public RtsPerformance() throws Throwable {
    }


    public static void main(String[] args) throws Throwable {
        RtsPerformance rtsPerformance = new RtsPerformance();
        rtsPerformance.setup();
        rtsPerformance.peformanceTest();

        rtsPerformance.destroy();
    }


    @Before
    public void setup() throws Throwable {
        File directory = new File(".");
        String currentPath = directory.getCanonicalPath();
        currentPath = currentPath + System.getProperty("file.separator") + cachePath;

        rtsUser1 = MIMCUser.newInstance(appAccount1, currentPath);
        rtsUser1.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlForToken, appAccount1));
        rtsUser1.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
            public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                        status, errType, errReason, errDescription);
            }
        });
        rtsUser1.registerMessageHandler(msgHandler1);
        rtsUser1.registerRtsCallHandler(callEventHandler1);

        rtsUser2 = MIMCUser.newInstance(appAccount2, currentPath);
        rtsUser2.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlForToken, appAccount2));
        rtsUser2.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
            public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                        status, errType, errReason, errDescription);
            }
        });
        rtsUser2.registerMessageHandler(msgHandler2);
        rtsUser2.registerRtsCallHandler(callEventHandler2);
    }

    @After
    public void destroy() throws Throwable {
        logOut(rtsUser1);
        logOut(rtsUser2);

        callEventHandler1.clear();
        callEventHandler2.clear();

        rtsUser1.destroy();
        rtsUser2.destroy();
    }

    @Test
    public void peformanceTest() throws Throwable {
        final int dataSizeKB = 40;
        final int speedKB = 80;
        final int durationSec = 60;
        testPerformance(rtsUser1, callEventHandler1, rtsUser2, callEventHandler2,
                dataSizeKB, speedKB, durationSec);
    }

    public static void testPerformance(MIMCUser user1, RtsPerformanceHandler callEventHandler1,
                                       MIMCUser user2, RtsPerformanceHandler callEventHandler2,
                                       int dataSizeKB, int dataSpeedKB, int durationSec) throws Throwable {
        int sendFailed = 0;
        int lost = 0;
        int dataError = 0;
        int time0To50 = 0;
        int time51To100 = 0;
        int time101To150 = 0;
        int time151To200 = 0;
        int time201To300 = 0;
        int time301To400 = 0;
        int time401To500 = 0;
        int time501To1000 = 0;
        int time1001More = 0;
        Random random = new Random();
        ConcurrentMap<Integer, RtsPerformanceData> sendDatas = new ConcurrentHashMap<Integer, RtsPerformanceData>();

        logIn(user1, callEventHandler1);
        logIn(user2, callEventHandler2);

        long chatId = createCall(user1, callEventHandler1, user2, callEventHandler2);

        final int COUNT = (int)((double)(durationSec * dataSpeedKB) / dataSizeKB);
        final int TIMEVAL_MS = (int)(1000.0 * dataSizeKB / dataSpeedKB);
        for (int dataId = 0; dataId < COUNT; dataId++) {
            byte[] sendData = new byte[dataSizeKB * 1024 - 4];
            random.nextBytes(sendData);
            sendData = byteMerge(intToByteArray(dataId), sendData);

            if (user1.sendRtsData(chatId, sendData, RtsDataType.AUDIO, XMDPacket.DataPriority.P0, true, 0, null, null) != -1) {
                sendDatas.put(dataId, new RtsPerformanceData(sendData, System.currentTimeMillis()));
            }

            Thread.sleep(TIMEVAL_MS);
        }

        Thread.sleep(5000);
        ConcurrentMap<Integer, RtsPerformanceData> recvDatas = callEventHandler2.pollDataInfo();
        closeCall(chatId, user1, callEventHandler1, callEventHandler2);

        for (int i = 0; i < COUNT; i++) {
            if (!sendDatas.containsKey(i)) {
                logger.warn("DATA_ID:{}, SEND FAILED", i);
                sendFailed++;
            } else if (!recvDatas.containsKey(i)) {
                logger.warn("DATA_ID:{}, LOST", i);
                lost++;
            } else if (!Arrays.equals(sendDatas.get(i).getData(), recvDatas.get(i).getData())) {
                logger.warn("DATA_ID:{}, RECEIVE DATA NOT MATCH", i);
                dataError++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 50) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time0To50++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 100) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time51To100++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 150) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time101To150++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 200) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time151To200++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 300) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time201To300++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 400) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time301To400++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 500) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time401To500++;
            } else if ((int)(recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime()) <= 1000) {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time501To1000++;
            } else {
                logger.info("DATA_ID:{}, {}-{}={}",
                    i, recvDatas.get(i).getDataTime(), sendDatas.get(i).getDataTime(), recvDatas.get(i).getDataTime() - sendDatas.get(i).getDataTime());
                time1001More++;
            }
        }

        logger.warn("\nPERFORMANCE TEST RESULT:\nSEND {} DATAS DURING {}s, DATA SIZE: {}KB, SPEED: {}KB/S\n" +
                "FAILED: {}\n" +
                "LOST: {}\n" +
                "DATA ERROR: {}\n" +
                "RECV_TIME(0, 50ms]: {}\n" +
                "RECV_TIME(50, 100ms]: {}\n" +
                "RECV_TIME(100, 150ms]: {}\n" +
                "RECV_TIME(150, 200ms]: {}\n" +
                "RECV_TIME(200, 300ms]: {}\n" +
                "RECV_TIME(300, 400ms]: {}\n" +
                "RECV_TIME(400, 500ms]: {}\n" +
                "RECV_TIME(500, 1000ms]: {}\n" +
                "RECV_TIME 1000+ms: {}\n",
            COUNT, durationSec, dataSizeKB, dataSpeedKB,
            sendFailed,
            lost,
            dataError,
            time0To50,
            time51To100,
            time101To150,
            time151To200,
            time201To300,
            time301To400,
            time401To500,
            time501To1000,
            time1001More);
    }

    public static byte[] intToByteArray(int n) {
        return new byte[] {
                (byte) ((n >> 24) & 0xFF),
                (byte) ((n >> 16) & 0xFF),
                (byte) ((n >> 8) & 0xFF),
                (byte) (n & 0xFF)
        };
    }

    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] byteMerge(byte[] b1, byte[] b2) {
        byte[] b3 = new byte[b1.length + b2.length];
        int i = 0;

        for(byte b: b1){
            b3[i] = b;
            i++;
        }

        for(byte b: b2){
            b3[i] = b;
            i++;
        }

        return b3;
    }

    static long createCall(MIMCUser from, RtsPerformanceHandler callEventHandlerFrom, MIMCUser to, RtsPerformanceHandler callEventHandlerTo) throws Throwable {
        Long chatId = from.dialCall(to.getAppAccount());
        Assert.assertNotNull("CREATE CALL, CHAT_ID IS NULL", chatId);
        Thread.sleep(100);

        RtsMessageData inviteRequest = callEventHandlerTo.pollInviteRequest(1000);
        Assert.assertNotNull("CREATE CALL, INVITE_REQUEST IS NULL", inviteRequest);
        Assert.assertEquals("CHAT_ID NOT MATCH IN INVITE_REQUEST", Long.valueOf(chatId), Long.valueOf(inviteRequest.getChatId()));
        Assert.assertEquals("APP_ACCOUNT NOT MATCH IN INVITE_REQUEST", from.getAppAccount(), inviteRequest.getFromAccount());
        Assert.assertEquals("RESOURCE NOT MATCH IN INVITE_REQUEST", from.getResource(), inviteRequest.getFromResource());
        Assert.assertEquals("APP_CONTENT NOT MATCH", true, ArrayUtils.isEmpty(inviteRequest.getAppContent()));

        RtsMessageData createResponse = callEventHandlerFrom.pollCreateResponse(1000);
        Assert.assertNotNull("CREATE_RESPONSE IS NULL", createResponse);
        Assert.assertEquals("CHAT_ID NOT MATCH IN CREATE_RESPONSE", Long.valueOf(chatId), Long.valueOf(createResponse.getChatId()));
        Assert.assertEquals("IS_ACCEPTED NOT MATCH IN CREATE_RESPONSE", true, createResponse.isAccepted());
        Assert.assertEquals("ERRMSG NOT MATCH IN CREATE_RESPONSE", RtsPerformanceHandler.LAUNCH_OK, createResponse.getExtramsg());

        return chatId;
    }

    static void closeCall(Long chatId, MIMCUser from, RtsPerformanceHandler callEventHandlerFrom, RtsPerformanceHandler callEventHandlerTo) throws Throwable {
        from.closeCall(chatId);
        Thread.sleep(100);

        RtsMessageData byeRequest = callEventHandlerTo.pollBye(1000);
        Assert.assertNotNull("BYE_REQUEST IS NULL", byeRequest);
        Assert.assertEquals("CHAT_ID NOT MATCH IN BYE_REQUEST", Long.valueOf(chatId), Long.valueOf(byeRequest.getChatId()));
        Assert.assertEquals("BYE_REASON NOT MATCH IN BYE_REQUEST", true, StringUtils.isEmpty(byeRequest.getExtramsg()));

        RtsMessageData byeResponse = callEventHandlerFrom.pollBye(1000);
        Assert.assertNotNull("BYE_RESPONSE IS NULL", byeResponse);
        Assert.assertEquals("CHAT_ID NOT MATCH IN BYE_RESPONSE", Long.valueOf(chatId), Long.valueOf(byeResponse.getChatId()));
        Assert.assertEquals("BYE_REASON NOT MATCH IN BYE_RESPONSE", "CLOSED_INITIATIVELY", byeResponse.getExtramsg());
    }

    static void logIn(MIMCUser user, RtsPerformanceHandler callEventHandler) throws Throwable {
        long currentTs = System.currentTimeMillis();
        user.login();
        while(System.currentTimeMillis()- currentTs < 5000 && !user.isOnline()) {
            Thread.sleep(50);
        }
        Assert.assertTrue("LOGIN FAILED", user.isOnline());

        if (callEventHandler != null) {
            logger.info("CLEAR CALLEVENTHANDLER OF UUID:{}", user.getUuid());
            callEventHandler.clear();
        }
    }

    static void logOut(MIMCUser user) throws Throwable {
        long currentTs = System.currentTimeMillis();
        user.logout();
        while(System.currentTimeMillis()- currentTs < 5000 && user.isOnline()) {
            Thread.sleep(50);
        }
        Assert.assertFalse("LOGOUT FAILED, USER STILL ONLINE", user.isOnline());
    }
}
