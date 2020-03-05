package RtsPerformanceTest;

import com.xiaomi.mimc.MIMCOnlineStatusListener;
import com.xiaomi.mimc.MIMCUser;
import com.xiaomi.mimc.common.MIMCConstant;
import com.xiaomi.mimc.data.DataPriority;
import com.xiaomi.mimc.data.RtsChannelType;
import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.logger.MIMCLog;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MIMCCaseMessageHandler;
import utils.MIMCCaseTokenFetcher;
import utils.RtsMessageData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RtsPerformance {
    private static final Logger logger = LoggerFactory.getLogger(RtsPerformance.class);

    public static final long appId = 2882303761517669588l;
    public static final String appKey = "5111766983588";
    public static final String appSecurity = "b0L3IOz/9Ob809v8H2FbVg==";
    public static final String urlForToken = "https://mimc.chat.xiaomi.net/api/account/token";


    private static final int WAIT_FOR_MESSAGE = 3000;
    private final String cachePath = "cacheFileRtsPerformance";

    public static final int callCount = 1;
    public static final int dataSizeKB = 2;
    public static final int speedKB = 22;
    public static final int durationSec = 60;
    public static final int packetLoss = 0;
    public static final RtsDataType dataType = RtsDataType.AUDIO;

    public static AtomicInteger sendFailed = new AtomicInteger();
    public static AtomicInteger lost = new AtomicInteger();
    public static AtomicInteger time0To50 = new AtomicInteger();
    public static AtomicInteger time51To100 = new AtomicInteger();
    public static AtomicInteger time101To150 = new AtomicInteger();
    public static AtomicInteger time151To200 = new AtomicInteger();
    public static AtomicInteger time201To300 = new AtomicInteger();
    public static AtomicInteger time301To400 = new AtomicInteger();
    public static AtomicInteger time401To500 = new AtomicInteger();
    public static AtomicInteger time501To1000 = new AtomicInteger();
    public static AtomicInteger time1001To2000 = new AtomicInteger();
    public static AtomicInteger time2001To5000 = new AtomicInteger();
    public static AtomicInteger time5001More = new AtomicInteger();

    public static AtomicInteger sendCount = new AtomicInteger();
    public static AtomicInteger succAckCount = new AtomicInteger();
    public static AtomicInteger failAckCount = new AtomicInteger();
    public static AtomicInteger recvCount = new AtomicInteger();

    private List<MIMCUser> fromUsers = new ArrayList<MIMCUser>();
    private List<MIMCUser> toUsers = new ArrayList<MIMCUser>();

    private final String fromAccountPrefix = "rts_performance_from";
    private final String toAccountPrefix = "rts_performance_to";

    private List<String> fromAccounts = new ArrayList<String>();
    private List<String> toAccounts = new ArrayList<String>();

    private MIMCCaseMessageHandler fromMsgHandler = new MIMCCaseMessageHandler();
    private MIMCCaseMessageHandler toMsgHandler = new MIMCCaseMessageHandler();

    private RtsPerformanceHandler fromCallEventHandler = new RtsPerformanceHandler();
    private RtsPerformanceHandler toCallEventHandler = new RtsPerformanceHandler();

    public RtsPerformance() throws Throwable {
    }

    @Before
    public void setup() throws Throwable {
        File directory = new File(".");
        String currentPath = directory.getCanonicalPath();
        currentPath = currentPath + System.getProperty("file.separator") + cachePath;

        MIMCLog.setLogger(new com.xiaomi.mimc.logger.Logger() {
            public void d(String s, String s1) {
                logger.debug("{}, {}", s, s1);
            }

            public void d(String s, String s1, Throwable throwable) {
                logger.debug("{}, {}", s, s1, throwable);
            }

            public void i(String s, String s1) {
                logger.info("{}, {}", s, s1);
            }

            public void i(String s, String s1, Throwable throwable) {
                logger.info("{}, {}", s, s1, throwable);
            }

            public void w(String s, String s1) {
                logger.warn("{}, {}", s, s1);
            }

            public void w(String s, String s1, Throwable throwable) {
                logger.warn("{}, {}", s, s1, throwable);
            }

            public void e(String s, String s1) {
                logger.error("{}, {}", s, s1);
            }

            public void e(String s, String s1, Throwable throwable) {
                logger.error("{}, {}", s, s1, throwable);
            }
        });

        //online init
        for (int i = 0; i < callCount; i++) {
            MIMCUser fromUser, toUser;

            fromUser = MIMCUser.newInstance(Long.valueOf(appId), fromAccountPrefix + i, "perfResource", currentPath, currentPath);
            fromUser.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlForToken, fromAccountPrefix + i));
            fromUser.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
                public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                    logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                            status, errType, errReason, errDescription);
                }
            });
            fromUser.registerMessageHandler(fromMsgHandler);
            fromUser.registerRtsCallHandler(fromCallEventHandler);

            toUser = MIMCUser.newInstance(Long.valueOf(appId), toAccountPrefix + i, "perfResource", currentPath, currentPath);
            toUser.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlForToken, toAccountPrefix + i));
            toUser.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
                public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                    logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                            status, errType, errReason, errDescription);
                }
            });
            toUser.registerMessageHandler(toMsgHandler);
            toUser.registerRtsCallHandler(toCallEventHandler);

            fromUsers.add(fromUser);
            toUsers.add(toUser);
            fromAccounts.add(fromAccountPrefix + i);
            toAccounts.add(toAccountPrefix + i);
        }

        //staging init
        /*
        for (int i = 0; i < callCount; i++) {
            MIMCUser fromUser, toUser;

            fromUser = MIMCUser.newInstance(Long.valueOf(appId), fromAccountPrefix + i, "perfResource", , currentPath, currentPath, urlForResolver, urlRoot);
            fromUser.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlToken, fromAccountPrefix + i));
            fromUser.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
                public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                    logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                            status, errType, errReason, errDescription);
                }
            });
            fromUser.registerMessageHandler(fromMsgHandler);
            fromUser.registerRtsCallHandler(fromCallEventHandler);

            toUser = MIMCUser.newInstance(Long.valueOf(appId), toAccountPrefix + i, "perfResource", currentPath, currentPath, urlForResolver, urlRoot);
            toUser.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlToken, toAccountPrefix + i));
            toUser.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
                public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                    logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                            status, errType, errReason, errDescription);
                }
            });
            toUser.registerMessageHandler(toMsgHandler);
            toUser.registerRtsCallHandler(toCallEventHandler);

            fromUsers.add(fromUser);
            toUsers.add(toUser);
            fromAccounts.add(fromAccountPrefix + i);
            toAccounts.add(toAccountPrefix + i);
        }
        */
    }

    @After
    public void destroy() throws Throwable {
        for (MIMCUser user : fromUsers) {
            user.destroy();
        }

        for (MIMCUser user : toUsers) {
            user.destroy();
        }
    }

    @Test
    public void peformanceTest() throws Throwable {
        testPerformanceRelay(fromUsers, fromCallEventHandler, toUsers, toCallEventHandler,
                dataSizeKB, speedKB, durationSec, packetLoss, dataType);
    }

    public static void testPerformanceRelay(List<MIMCUser> fromUsers, RtsPerformanceHandler fromCallEventHandler,
                                            List<MIMCUser> toUsers, RtsPerformanceHandler toCallEventHandler,
                                            int dataSizeKB, int dataSpeedKB, int durationSec,
                                            int packetLoss, RtsDataType dataType) throws Throwable {
        Random random = new Random();
        List<Long> chatIds = new ArrayList<Long>();

        for (MIMCUser user : fromUsers) {
            logIn(user, fromCallEventHandler);
        }

        for (MIMCUser user : toUsers) {
            logIn(user, toCallEventHandler);
        }

        while (true) {
            int recvCnt = recvCount.get();
            Thread.sleep(1000);
            System.out.println(String.format("recv old datas. received: %s ... %s", recvCnt, recvCount.get()));
            if (recvCnt == recvCount.get()) break;
        }

        resetStat();

        for (int i = 0; i < fromUsers.size(); i++) {
            long chatId = createCall(fromUsers.get(i), fromCallEventHandler, toUsers.get(i), toCallEventHandler);
            fromUsers.get(i).setPacketLossRate(packetLoss);
            chatIds.add(chatId);
        }

        long startTime = System.currentTimeMillis();
        long nextSendTime = startTime;

        final int COUNT = (int)((double)(durationSec * dataSpeedKB) / dataSizeKB);
        for (int n = 0; n < COUNT; n++) {
            byte[] sendData = new byte[dataSizeKB * 1024 - 8];
            random.nextBytes(sendData);
            Object context = System.currentTimeMillis();

            long now = System.currentTimeMillis();
            if (now < nextSendTime) {
                Thread.sleep(nextSendTime - now);
            }

            for (int i = 0; i < fromUsers.size(); i++) {
                if (fromUsers.get(i).sendRtsData(chatIds.get(i), byteMerge(longToBytes(System.currentTimeMillis()), sendData), dataType,
                        DataPriority.P0, false, 3, RtsChannelType.RELAY, context) == -1) {
                    logger.warn("SEND DATA FAILED, CHAI_ID {}", chatIds.get(i));
                    sendFailed.addAndGet(1);
                } else {
                    sendCount.addAndGet(1);
                    System.out.println(String.format("SEND DATA IN CHAT_ID %s, SEND_COUNT %s", chatIds.get(i), sendCount));
                }
            }

            nextSendTime = 1000 * dataSizeKB / dataSpeedKB + nextSendTime;
        }

        long finishTime = System.currentTimeMillis();

        while (true) {
            int recvCnt = recvCount.get();
            Thread.sleep(1000);
            System.out.println(String.format("recv datas. received: %s ... %s", recvCnt, recvCount.get()));
            if (recvCnt == recvCount.get()) break;
        }

        for (int i = 0; i < fromUsers.size(); i++) {
            closeCall(chatIds.get(i), fromUsers.get(i), fromCallEventHandler, toCallEventHandler);
        }

        lost.set(COUNT * chatIds.size() - sendFailed.get() - time0To50.get() - time51To100.get() - time101To150.get() - time151To200.get() - time201To300.get()
                - time301To400.get() - time401To500.get() - time501To1000.get() - time1001To2000.get() - time2001To5000.get() - time5001More.get());

        logger.warn("\nPERFORMANCE TEST RESULT:\nSEND {} {} DATAS ON {} CALLS DURING {}s, DATA SIZE: {}KB, SPEED: {}*{}KB/S\n" +
                        "SEND_SUCC: {}\n" +
                        "SEND_FAIL: {}\n" +
                        "SUCC_ACK: {}\n" +
                        "FAIL_ACK: {}\n" +
                        "RECV: {}\n" +
                        "LOST: {}\n" +
                        "RECV_TIME(0, 50ms]: {}\n" +
                        "RECV_TIME(50, 100ms]: {}\n" +
                        "RECV_TIME(100, 150ms]: {}\n" +
                        "RECV_TIME(150, 200ms]: {}\n" +
                        "RECV_TIME(200, 300ms]: {}\n" +
                        "RECV_TIME(300, 400ms]: {}\n" +
                        "RECV_TIME(400, 500ms]: {}\n" +
                        "RECV_TIME(500, 1000ms]: {}\n" +
                        "RECV_TIME(1001, 2000ms]: {}\n" +
                        "RECV_TIME(2001, 5000ms]: {}\n" +
                        "RECV_TIME 5000+ms: {}\n",
                COUNT * chatIds.size(), dataType, chatIds.size(), (finishTime - startTime) / 1000.0, dataSizeKB, chatIds.size(), dataSpeedKB,
                sendCount,
                sendFailed,
                succAckCount,
                failAckCount,
                recvCount,
                lost,
                time0To50,
                time51To100,
                time101To150,
                time151To200,
                time201To300,
                time301To400,
                time401To500,
                time501To1000,
                time1001To2000,
                time2001To5000,
                time5001More);

        Assert.assertEquals(sendCount.get(), succAckCount.get());
        Assert.assertEquals(sendCount.get(), recvCount.get());
    }

    public static void resetStat() {
        sendFailed.set(0);
        lost.set(0);
        time0To50.set(0);
        time51To100.set(0);
        time101To150.set(0);
        time151To200.set(0);
        time201To300.set(0);
        time301To400.set(0);
        time401To500.set(0);
        time501To1000.set(0);
        time1001To2000.set(0);
        time2001To5000.set(0);
        time5001More.set(0);

        sendCount.set(0);
        succAckCount.set(0);
        failAckCount.set(0);
        recvCount.set(0);
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

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong(byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
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

    public static synchronized void checkRecvDataTime(byte[] recvData, long recvTime) {
        long t = recvTime - bytesToLong(recvData);
        if (t <= 50) {
            time0To50.addAndGet(1);
        } else if (t <= 100) {
            time51To100.addAndGet(1);
        } else if (t <= 150) {
            time101To150.addAndGet(1);
        } else if (t <= 200) {
            time151To200.addAndGet(1);
        } else if (t <= 300) {
            time201To300.addAndGet(1);
        } else if (t <= 400) {
            time301To400.addAndGet(1);
        } else if (t <= 500) {
            time401To500.addAndGet(1);
        } else if (t <= 1000) {
            time501To1000.addAndGet(1);
        } else if (t <= 2000) {
            time1001To2000.addAndGet(1);
        } else if (t <= 5000) {
            time2001To5000.addAndGet(1);
        } else {
            time5001More.addAndGet(1);
        }
    }

    static long createCall(MIMCUser from, RtsPerformanceHandler callEventHandlerFrom, MIMCUser to, RtsPerformanceHandler callEventHandlerTo) throws Throwable {
        long chatId = from.dialCall(to.getAppAccount());
        Assert.assertNotEquals("CREATE CALL, INVALID CHAT_ID", -1, chatId);

        RtsMessageData inviteRequest = callEventHandlerTo.pollInviteRequest(WAIT_FOR_MESSAGE);
        Assert.assertNotNull("CREATE CALL, INVITE_REQUEST IS NULL", inviteRequest);
        Assert.assertEquals("CHAT_ID NOT MATCH IN INVITE_REQUEST", Long.valueOf(chatId), Long.valueOf(inviteRequest.getChatId()));
        Assert.assertEquals("APP_ACCOUNT NOT MATCH IN INVITE_REQUEST", from.getAppAccount(), inviteRequest.getFromAccount());
        Assert.assertEquals("RESOURCE NOT MATCH IN INVITE_REQUEST", from.getResource(), inviteRequest.getFromResource());
        Assert.assertEquals("APP_CONTENT NOT MATCH", true, ArrayUtils.isEmpty(inviteRequest.getAppContent()));

        RtsMessageData createResponse = callEventHandlerFrom.pollCreateResponse(WAIT_FOR_MESSAGE);
        Assert.assertNotNull("CREATE_RESPONSE IS NULL", createResponse);
        Assert.assertEquals("CHAT_ID NOT MATCH IN CREATE_RESPONSE", Long.valueOf(chatId), Long.valueOf(createResponse.getChatId()));
        Assert.assertEquals("IS_ACCEPTED NOT MATCH IN CREATE_RESPONSE", true, createResponse.isAccepted());
        Assert.assertEquals("ERRMSG NOT MATCH IN CREATE_RESPONSE", RtsPerformanceHandler.LAUNCH_OK, createResponse.getExtramsg());

        return chatId;
    }

    static void closeCall(Long chatId, MIMCUser from, RtsPerformanceHandler callEventHandlerFrom, RtsPerformanceHandler callEventHandlerTo) throws Throwable {
        from.closeCall(chatId);

        RtsMessageData byeRequest = callEventHandlerTo.pollBye(WAIT_FOR_MESSAGE);
        Assert.assertNotNull("BYE_REQUEST IS NULL", byeRequest);
        Assert.assertEquals("CHAT_ID NOT MATCH IN BYE_REQUEST", Long.valueOf(chatId), Long.valueOf(byeRequest.getChatId()));
        Assert.assertEquals("BYE_REASON NOT MATCH IN BYE_REQUEST", true, StringUtils.isEmpty(byeRequest.getExtramsg()));

        RtsMessageData byeResponse = callEventHandlerFrom.pollBye(WAIT_FOR_MESSAGE);
        Assert.assertNotNull("BYE_RESPONSE IS NULL", byeResponse);
        Assert.assertEquals("CHAT_ID NOT MATCH IN BYE_RESPONSE", Long.valueOf(chatId), Long.valueOf(byeResponse.getChatId()));
        Assert.assertEquals("BYE_REASON NOT MATCH IN BYE_RESPONSE", "CLOSED_INITIATIVELY", byeResponse.getExtramsg());
    }

    static void logIn(MIMCUser user, RtsPerformanceHandler callEventHandler) throws Throwable {
        long currentTs = System.currentTimeMillis();
        user.login();
        while(System.currentTimeMillis()- currentTs < 5000 && !user.isOnline()) {
            Thread.sleep(10);
        }
        Assert.assertTrue("LOGIN FAILED", user.isOnline());

        if (callEventHandler != null) {
            logger.info("CLEAR CALLEVENTHANDLER OF UUID:{}", user.getUuid());
            callEventHandler.clear();
        }
    }
}
