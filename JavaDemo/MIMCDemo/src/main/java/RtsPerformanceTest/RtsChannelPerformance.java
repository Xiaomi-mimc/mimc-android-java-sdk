package RtsPerformanceTest;

import com.xiaomi.mimc.MIMCOnlineStatusListener;
import com.xiaomi.mimc.MIMCRtsChannelHandler;
import com.xiaomi.mimc.MIMCUser;
import com.xiaomi.mimc.common.MIMCConstant;
import com.xiaomi.mimc.data.ChannelUser;
import com.xiaomi.mimc.data.DataPriority;
import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.logger.MIMCLog;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MIMCCaseMessageHandler;
import utils.MIMCCaseTokenFetcher;
import utils.RtsChannelData;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RtsChannelPerformance {
    private static final Logger logger = LoggerFactory.getLogger(RtsChannelPerformance.class);

    public static final long appId = 2882303761517669588l;
    public static final String appKey = "5111766983588";
    public static final String appSecurity = "b0L3IOz/9Ob809v8H2FbVg==";
    public static final String urlForToken = "https://mimc.chat.xiaomi.net/api/account/token";

    private static final int WAIT_FOR_MESSAGE = 3000;
    private final String cachePath = "cacheFileRtsPerformance";

    public static final int channelMemberCount = 4; //channel成员个数
    public static final int dataSizeKB = 3; //单个数据大小
    public static final int speedKB = 30; //每秒发送的数据总大小
    public static final int durationSec = 10; //运行时间
    public static final int packetLoss = 0;
    public static final RtsDataType dataType = RtsDataType.AUDIO;

    public AtomicInteger sendCount = new AtomicInteger();
    public AtomicInteger sendSuccess = new AtomicInteger();
    public AtomicInteger sendFailed = new AtomicInteger();

    private List<MIMCUser> channelUsers = new ArrayList<MIMCUser>();
    private final String channelAccountPrefix = "rts_performance_channelUser_zhy";
    private List<String> channelAccounts = new ArrayList<String>();

    private MIMCCaseMessageHandler channelMsgHandler = new MIMCCaseMessageHandler();
    private Map<String, RtsChannelPerformanceHandler> channelEventHandlers = new HashMap<String, RtsChannelPerformanceHandler>();

    public RtsChannelPerformance() throws Throwable {
    }

    @Before
    public void setup() throws Throwable {
        File directory = new File(".");
        String currentPath = directory.getCanonicalPath();
        currentPath = currentPath + System.getProperty("file.separator") + cachePath;

        MIMCLog.setLogger(new com.xiaomi.mimc.logger.Logger() {
            public void d(String s, String s1) {

            }

            public void d(String s, String s1, Throwable throwable) {

            }

            public void i(String s, String s1) {

            }

            public void i(String s, String s1, Throwable throwable) {

            }

            public void w(String s, String s1) {

            }

            public void w(String s, String s1, Throwable throwable) {

            }

            public void e(String s, String s1) {

            }

            public void e(String s, String s1, Throwable throwable) {

            }
            });

        for (int i = 0; i < channelMemberCount; i++) {
            MIMCUser user;
            String appAccount = channelAccountPrefix + i;
            RtsChannelPerformanceHandler channelEventHandler = new RtsChannelPerformanceHandler(appAccount);

            user = MIMCUser.newInstance(appId, appAccount, "perfResource", currentPath, currentPath);
            user.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, urlForToken, appAccount));
            user.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
                public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                    logger.info("OnlineStatusHandler, Called, isOnline:{}, errType:{}, errReason:{}, errDesc:{}",
                            status, errType, errReason, errDescription);
                }
            });
            user.registerMessageHandler(channelMsgHandler);
            user.registerChannelHandler(channelEventHandler);

            channelUsers.add(user);
            channelAccounts.add(appAccount);
            channelEventHandlers.put(appAccount, channelEventHandler);
        }
    }

    @After
    public void destroy() throws Throwable {
        for (MIMCUser user : channelUsers) {
            user.destroy();
        }
    }

    @Test
    public void channelPerformanceTest() throws Throwable {
        testPerformanceChannel(channelUsers, channelEventHandlers, dataSizeKB, speedKB, durationSec, packetLoss, dataType);
    }

    public void testPerformanceChannel(List<MIMCUser> users, Map<String, RtsChannelPerformanceHandler> channelHandlers,
                                       int dataSizeKB, int dataSpeedKB, int durationSec, int packetLoss, RtsDataType dataType) throws Throwable {
        Random random = new Random();
        byte[] extra = "channel for performance test".getBytes();
        MIMCUser creator = users.get(0);
        RtsChannelData channelInfo = createChannel(creator, channelHandlers.get(creator.getAppAccount()), extra);
        long callId = channelInfo.getCallId();
        String callKey = channelInfo.getCallKey();

        for (int i = 0; i < users.size(); i++) {
            users.get(i).setPacketLossRate(packetLoss);
            if (i > 0) {
                Assert.assertTrue(users.get(i).getAppAccount() + " JOIN CHANNEL FAIL",
                    joinChannel(users.get(i), channelHandlers.get(users.get(i).getAppAccount()), callId, callKey).isSuccess());
            }
        }

        while (true) {
            int recvCnt1 = 0;
            for (String account : channelEventHandlers.keySet()) {
                recvCnt1 += channelHandlers.get(account).getRecvCount().get();
            }

            Thread.sleep(1000);
            int recvCnt2 = 0;
            for (String account : channelEventHandlers.keySet()) {
                recvCnt2 += channelHandlers.get(account).getRecvCount().get();
            }

            System.out.println(String.format("recv old datas. received: %s ... %s", recvCnt1, recvCnt2));
            if (recvCnt1 == recvCnt2) break;
        }

        resetStat();

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

            if (users.get(n % users.size()).sendRtsData(callId, byteMerge(longToBytes(System.currentTimeMillis()), sendData),
                    dataType, DataPriority.P0, false, 3, context) == -1) {
                logger.warn("\n{} SEND CHANNEL DATA FAILED, CALL_ID {}", users.get(n % users.size()).getAppAccount(), callId);
                sendFailed.addAndGet(1);
            } else {
                sendSuccess.addAndGet(1);
                System.out.println(String.format("%s SEND DATA, SEND_COUNT %s", users.get(n % users.size()).getAppAccount(), sendSuccess));
            }
            sendCount.addAndGet(1);

            nextSendTime = 1000 * durationSec / (COUNT - 1) + nextSendTime;
        }

        long finishTime = System.currentTimeMillis();

        while (true) {
            int recvCnt1 = 0;
            for (String account : channelEventHandlers.keySet()) {
                recvCnt1 += channelHandlers.get(account).getRecvCount().get();
            }

            Thread.sleep(1000);
            int recvCnt2 = 0;
            for (String account : channelEventHandlers.keySet()) {
                recvCnt2 += channelHandlers.get(account).getRecvCount().get();
            }

            System.out.println(String.format("recv old datas. received: %s ... %s", recvCnt1, recvCnt2));
            if (recvCnt1 == recvCnt2) break;
        }

        int expectRecv = sendCount.get() * (users.size() - 1);
        int actualRecv = 0;
        for (String account : channelEventHandlers.keySet()) {
            actualRecv += channelHandlers.get(account).getRecvCount().get();
        }

        logger.warn("\nPERFORMANCE TEST RESULT:\n" +
                    "SEND {} DATAS IN {} MEMBER CHANNEL DURING {} Sec, DATA_SIZE: {}KB, DATA_SPEED: {} KB/S\n" +
                    "SEND_SUCCESS: {}, SEND_FAILD: {}\n" +
                    "EXPECT_RECV: {}, ACTUAL_RECV: {}, LOST: {}",
                    sendCount, users.size(), (finishTime - startTime) / 1000.0, dataSizeKB, dataSpeedKB,
                    sendSuccess, sendFailed,
                    expectRecv, actualRecv, expectRecv - actualRecv);

        for (String account: channelHandlers.keySet()) {
            channelHandlers.get(account).print();
        }
    }

    public void resetStat() {
        sendCount.set(0);
        sendFailed.set(0);
        sendSuccess.set(0);

        for (String account : channelEventHandlers.keySet()) {
            channelEventHandlers.get(account).resetStat();
        }
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

    public static RtsChannelData createChannel(MIMCUser user, RtsChannelPerformanceHandler channelHandler, byte[] extra) throws Throwable {
        logIn(user);
        channelHandler.clear();
        user.createChannel(extra);

        RtsChannelData result = channelHandler.pollCreate(WAIT_FOR_MESSAGE);
        Assert.assertNotNull("CREATE CHANNEL RESULT IS NULL", result);
        Assert.assertTrue("CREATE CHANNEL FAILED", result.isSuccess());
        Assert.assertTrue("CREATE CHANNEL, INVALID CALL_ID", result.getCallId() > 0);
        Assert.assertTrue("CREATE CHANNEL, INVALID CALL_KEY", result.getCallKey().length() > 0);
        Assert.assertTrue("CREATE CHANNEL, INVALID IDENTITY", result.getIdentity() > 0);
        Assert.assertTrue("CREATE CHANNEL, EXTRA NOT MATCH", Arrays.equals(extra, result.getExtra()));

        List<ChannelUser> members = user.getChannelUsers(result.getCallId());
        Assert.assertEquals("CHANNEL MEMBER COUNT NOT 1 AFTER CREATE", 1, members.size());
        Assert.assertEquals("CREATE CHANNEL, MEMBER ACCOUNT NOT MATCH.", user.getAppAccount(), members.get(0).getAppAccount());
        Assert.assertEquals("CREATE CHANNEL, MEMBER RESOURCE NOT MATCH.", user.getResource(), members.get(0).getResource());

        return result;
    }

    public static RtsChannelData joinChannel(MIMCUser user, RtsChannelPerformanceHandler channelHandler, long callId, String callKey) throws Throwable {
        logIn(user);
        channelHandler.clear();
        user.joinChannel(callId, callKey);
        String assertMsg = String.format(" CALL_ID: %s, UUID: %s, ACCOUNT: %s, RESOURCE: %s", callId, user.getUuid(), user.getAppAccount(), user.getResource());

        RtsChannelData result = channelHandler.pollJoin(WAIT_FOR_MESSAGE);
        Assert.assertNotNull("JOIN CHANNEL RESULT IS NULL." + assertMsg, result);
        Assert.assertEquals("JOIN CHANNEL, CALL_ID NOT MATCH." + assertMsg, callId, result.getCallId());
        Assert.assertEquals("JOIN CHANNEL, ACCOUNT NOT MATCH." + assertMsg, user.getAppAccount(), result.getAccount());
        Assert.assertEquals("JOIN CHANNEL, RESOURCE NOT MATCH." + assertMsg, user.getResource(), result.getResource());

        if (result.isSuccess()) {
            List<ChannelUser> membersInJoinResult = result.getMembers();
            List<ChannelUser> membersInQueryResult = user.getChannelUsers(callId);
            Assert.assertEquals("MEMBER COUNT NOT EQUALS IN JOIN RESULT AND QUERY RESULT", membersInJoinResult.size(), membersInQueryResult.size());

            int i = 0;
            String membersInfo = "\nmembers of callId: " + callId;
            for (int n = 0; n < membersInJoinResult.size(); n++) {
                membersInfo = String.format("%s\naccount: %s, resource: %s", membersInfo, membersInJoinResult.get(n).getAppAccount(), membersInJoinResult.get(n).getResource());
                Assert.assertEquals("USER ACCOUNT NOT EQUALS IN JOIN RESULT AND QUERY RESULT",
                        membersInJoinResult.get(n).getAppAccount(), membersInQueryResult.get(n).getAppAccount());
                Assert.assertEquals("USER RESOURCE NOT EQUALS IN JOIN RESULT AND QUERY RESULT",
                        membersInJoinResult.get(n).getResource(), membersInQueryResult.get(n).getResource());
                if (membersInJoinResult.get(n).getAppAccount().equals(user.getAppAccount()) && membersInJoinResult.get(n).getResource().equals(user.getResource())) {
                    i++;
                }
            }
            logger.info(membersInfo);
            Assert.assertEquals("USER NOT IN MEMBER LIST AFTER JOIN CHANNEL", 1, i);
        }

        return result;
    }

    public static void logIn(MIMCUser user) throws Throwable {
        long currentTs = System.currentTimeMillis();
        user.login();
        while(System.currentTimeMillis()- currentTs < 5000 && !user.isOnline()) {
            Thread.sleep(10);
        }
        Assert.assertTrue("LOGIN FAILED", user.isOnline());
    }

    public static class RtsChannelPerformanceHandler implements MIMCRtsChannelHandler {
        private String account;

        private AtomicInteger succAckCount = new AtomicInteger();
        private AtomicInteger failAckCount = new AtomicInteger();
        private AtomicInteger recvCount = new AtomicInteger();
        private AtomicInteger time0To50 = new AtomicInteger();
        private AtomicInteger time51To100 = new AtomicInteger();
        private AtomicInteger time101To150 = new AtomicInteger();
        private AtomicInteger time151To200 = new AtomicInteger();
        private AtomicInteger time201To300 = new AtomicInteger();
        private AtomicInteger time301To400 = new AtomicInteger();
        private AtomicInteger time401To500 = new AtomicInteger();
        private AtomicInteger time501To1000 = new AtomicInteger();
        private AtomicInteger time1001To2000 = new AtomicInteger();
        private AtomicInteger time2001To5000 = new AtomicInteger();
        private AtomicInteger time5001More = new AtomicInteger();

        private BlockingQueue<RtsChannelData> create = new LinkedBlockingQueue<RtsChannelData>();
        private BlockingQueue<RtsChannelData> join = new LinkedBlockingQueue<RtsChannelData>();

        public RtsChannelPerformanceHandler(String account) {
            this.account = account;
        }

        public void onCreateChannel(long identity, long callId, String callKey, boolean success, String desc, byte[] extra) {
            logger.info("onCreateChannel identity:{}, callId:{}, callKey:{}, success:{}, desc:{}, extra:{}",
                    identity, callId, callKey, success, desc, extra);
            create.add(new RtsChannelData(identity, callId, callKey, success, desc, extra));
        }

        public void onJoinChannel(long callId, String account, String resource, boolean success, String desc, byte[] extra, List<ChannelUser> members) {
            logger.info("onJoinChannel callId:{}, account:{}, resource:{}, success:{}, desc:{}, extra:{}, members:{}",
                    callId, account, resource, success, desc, extra, members);
            join.add(new RtsChannelData(callId, account, resource, success, desc, extra, members));
        }

        public void onLeaveChannel(long callId, String account, String resource, boolean success, String desc) {
            logger.info("onLeaveChannel callId:{}, account:{}, resource:{}, success:{}, desc:{}",
                    callId, account, resource, success, desc);
        }

        public void onUserJoined(long callId, String account, String resource) {
            logger.info("onUserJoined callId:{}, account:{}, resource:{}", callId, account, resource);
        }

        public void onUserLeft(long callId, String account, String resource) {
            logger.info("onUserLeft callId:{}, account:{}, resource:{}", callId, account, resource);
        }

        public void onData(long callId, String account, String resource, byte[] audioData, RtsDataType data_type) {
            logger.info("ReceiveRtsChannelData, callId:{}, account:{}, resource:{}, data_type:{}, dataLen:{}",
                    callId, account, resource, data_type, audioData.length);
            recvCount.addAndGet(1);
            checkRecvDataTime(audioData, System.currentTimeMillis());
        }

        public void onSendDataSuccess(long callId, int groupId, Object context) {
            logger.info("SendRtsChannelDataSuccess, callId:{}, groupId:{}, object:{}", callId, groupId, context);
            succAckCount.addAndGet(1);
        }

        public void onSendDataFailure(long callId, int groupId, Object context) {
            logger.info("SendRtsChannelDataFail, callId:{}, groupId:{}, object:{}", callId, groupId, context);
            failAckCount.addAndGet(1);
        }

        public RtsChannelData pollCreate(long timeoutMs) {
            try {
                RtsChannelData result = create.poll(timeoutMs, TimeUnit.MILLISECONDS);
                logger.info("POLL_CREATE_CHANNEL_RESPONSE, result:{}", result);
                return result;
            } catch (Exception e) {
                logger.warn("Exception:{}", e);
                return null;
            }
        }

        public RtsChannelData pollJoin(long timeoutMs) {
            try {
                RtsChannelData result = join.poll(timeoutMs, TimeUnit.MILLISECONDS);
                logger.info("POLL_JOIN_CHANNEL_RESPONSE, result:{}", result);
                return result;
            } catch (Exception e) {
                logger.warn("Exception:{}", e);
                return null;
            }
        }

        public AtomicInteger getRecvCount() {
            return recvCount;
        }

        public synchronized void checkRecvDataTime(byte[] recvData, long recvTime) {
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

        public void print() {
            logger.warn("\n{}:SEND_SUCC: {}, SEND_FAIL: {}, RECEIVE {} DATAS\n" +
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
                    account, succAckCount, failAckCount, recvCount,
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
        }

        public void clear() {
            create.clear();
            join.clear();
        }

        public void resetStat() {
            succAckCount.set(0);
            failAckCount.set(0);
            recvCount.set(0);
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
        }
    }
}
