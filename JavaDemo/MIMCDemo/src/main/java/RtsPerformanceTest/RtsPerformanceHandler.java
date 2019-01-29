package RtsPerformanceTest;

import com.xiaomi.mimc.MIMCRtsCallHandler;
import com.xiaomi.mimc.data.LaunchedResponse;
import com.xiaomi.mimc.data.RtsChannelType;
import com.xiaomi.mimc.data.RtsDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.RtsMessageData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RtsPerformanceHandler implements MIMCRtsCallHandler {
    private static final Logger logger = LoggerFactory.getLogger(RtsPerformanceHandler.class);

    private BlockingQueue<RtsMessageData> inviteRequest = new LinkedBlockingQueue<RtsMessageData>();
    private BlockingQueue<RtsMessageData> createResponse = new LinkedBlockingQueue<RtsMessageData>();
    private BlockingQueue<RtsMessageData> bye = new LinkedBlockingQueue<RtsMessageData>();

    public final static String LAUNCH_OK = "ok";

    public LaunchedResponse onLaunched(String fromAccount, String fromResource, long chatId, byte[] data) {
        logger.info("onLaunched fromAccount:{}, fromResource:{}, chatId:{}", fromAccount, fromResource, chatId);
        inviteRequest.add(new RtsMessageData(fromAccount, fromResource, chatId, data));
        return new LaunchedResponse(true, LAUNCH_OK);
    }

    public void onAnswered(long chatId, boolean accepted, String errmsg) {
        logger.info("onAnswered chatId:{}, accepted:{}, errmsg:{}", chatId, accepted, errmsg);
        createResponse.add(new RtsMessageData(chatId, accepted, errmsg));
    }

    public void onClosed(long chatId, String extramsg) {
        logger.info("onClosed chatId:{}, errmsg:{}", chatId, extramsg);
        logger.debug("In onClosed before add bye.size:{}", bye.size());
        bye.add(new RtsMessageData(chatId, extramsg));
        logger.debug("In onClosed after add bye.size:{}", bye.size());
    }

    public synchronized void onData(long callId, String fromAccount, String resource, byte[] data, RtsDataType dataType, RtsChannelType channelType) {
        logger.info("ReceiveRtsData, chatId:{}, channel_type:{} ,pkt_type:{}, dataLen:{}",
                callId, channelType, dataType, data.length);
        RtsPerformance.recvCount.addAndGet(1);
        RtsPerformance.checkRecvDataTime(data, System.currentTimeMillis());
    }

    public synchronized void onSendDataSuccess(long callId, int dataId, Object context) {
        RtsPerformance.succAckCount.addAndGet(1);
    }

    public synchronized void onSendDataFailure(long l, int i, Object o) {
        RtsPerformance.failAckCount.addAndGet(1);
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

    public boolean clear() {
        inviteRequest.clear();
        createResponse.clear();
        bye.clear();
        return true;
    }
}
