package com.xiaomi.mimcdemo.performanceTest;

import com.xiaomi.mimc.MIMCRtsCallHandler;
import com.xiaomi.mimc.data.RtsChannelType;
import com.xiaomi.mimcdemo.performanceTest.utils.RtsMessageData;
import com.xiaomi.mimc.data.LaunchedResponse;
import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimc.proto.RtsData;
import com.xiaomi.mimc.proto.RtsSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class RtsPerformanceHandler implements MIMCRtsCallHandler {
    private static final Logger logger = LoggerFactory.getLogger(RtsPerformanceHandler.class);

    private BlockingQueue<RtsMessageData> inviteRequest = new LinkedBlockingQueue<RtsMessageData>();
    private BlockingQueue<RtsMessageData> createResponse = new LinkedBlockingQueue<RtsMessageData>();
    private BlockingQueue<RtsMessageData> bye = new LinkedBlockingQueue<RtsMessageData>();
    private ConcurrentMap<Integer, RtsPerformanceData> recvData = new ConcurrentHashMap<Integer, RtsPerformanceData>();

    public final static String LAUNCH_OK = "ok";

    public LaunchedResponse onLaunched(String fromAccount, String fromResource, Long chatId, RtsSignal.StreamDataType dataType, byte[] data) {
        logger.info("onLaunched fromAccount:{}, fromResource:{}, chatId:{}", fromAccount, fromResource, chatId);
        inviteRequest.add(new RtsMessageData(fromAccount, fromResource, chatId, data));
        return new LaunchedResponse(true, LAUNCH_OK);
    }

    public void onAnswered(Long chatId, boolean accepted, String errmsg) {
        logger.info("onAnswered chatId:{}, accepted:{}, errmsg:{}", chatId, accepted, errmsg);
        createResponse.add(new RtsMessageData(chatId, accepted, errmsg));
    }

    public void onClosed(Long chatId, String extramsg) {
        logger.info("onClosed chatId:{}, errmsg:{}", chatId, extramsg);
        logger.debug("In onClosed before add bye.size:{}", bye.size());
        bye.add(new RtsMessageData(chatId, extramsg));
        logger.debug("In onClosed after add bye.size:{}", bye.size());
    }

    public void handleData(Long chatId, byte[] audioData, RtsDataType dataType, RtsChannelType channelType) {
        logger.info("ReceiveRtsData, chatId:{}, channel_type:{} ,pkt_type:{}, dataLen:{}",
                chatId, channelType, dataType, audioData.length);
        int dataId = RtsPerformance.byteArrayToInt(audioData);
        recvData.put(dataId, new RtsPerformanceData(audioData, System.currentTimeMillis()));
    }

    public RtsMessageData pollInviteRequest(long timeoutMs) {
        try {
            RtsMessageData rtsMessageData = inviteRequest.poll(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("POLL_INVITE_REQUEST, rtsMessageData:{}", rtsMessageData);
            return rtsMessageData;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public RtsMessageData pollCreateResponse(long timeoutMs) {
        try {
            RtsMessageData rtsMessageData = createResponse.poll(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("POLL_CREATE_RESPONSE, rtsMessageData:{}", rtsMessageData);
            return rtsMessageData;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public RtsMessageData pollBye(long timeoutMs) {
        try {
            logger.debug("In pollBye before poll bye.size:{}", bye.size());
            RtsMessageData rtsMessageData = bye.poll(timeoutMs, TimeUnit.MILLISECONDS);
            logger.debug("In pollBye after poll bye.size:{}", bye.size());
            if (bye != null && bye.size() != 0) {
                RtsMessageData rtsMessageData2 = bye.poll(timeoutMs, TimeUnit.MILLISECONDS);
                logger.info("chatId:{}", rtsMessageData2.getChatId());
            }
            logger.info("POLL_BYE, rtsMessageData:{}", rtsMessageData);
            return rtsMessageData;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public ConcurrentMap pollDataInfo() {
        try {
            logger.debug("In pollDataInfo, data.size:{}", recvData.size());
            return recvData;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public int getMsgSize(int msgType) {
        int size = -1;

        switch (msgType) {
            case 1: size = inviteRequest.size(); break;
            case 2: size = createResponse.size(); break;
            case 3: size = bye.size(); break;
            case 4: size = recvData.size(); break;
            default: break;
        }

        return size;
    }

    public boolean clear() {
        inviteRequest.clear();
        createResponse.clear();
        bye.clear();
        recvData.clear();
        return true;
    }
}
