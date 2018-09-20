package com.xiaomi.mimcdemo.performanceTest;


import com.xiaomi.mimc.MIMCGroupMessage;
import com.xiaomi.mimc.MIMCMessage;
import com.xiaomi.mimc.MIMCMessageHandler;
import com.xiaomi.mimc.MIMCServerAck;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MIMCCaseMessageHandler implements MIMCMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MIMCCaseMessageHandler.class);

    private BlockingQueue<MIMCMessage> messages = new LinkedBlockingQueue<MIMCMessage>();
    private BlockingQueue<MIMCGroupMessage> groupMessages = new LinkedBlockingQueue<MIMCGroupMessage>();
    private BlockingQueue<MIMCServerAck> serverAcks = new LinkedBlockingQueue<MIMCServerAck>();
    private BlockingQueue<MIMCGroupMessage> ucGroupMessages = new LinkedBlockingQueue<MIMCGroupMessage>();

    public void handleMessage(List<MIMCMessage> packets) {
        logger.info("RECV_MIMC_MESSAGE, MIMC HandleMessage Called.");

        try {
            messages.addAll(packets);
            logger.info("RECV_MIMC_MESSAGE, MESSAGES_SIZE:{}", messages.size());
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
        }
    }

    public void handleGroupMessage(List<MIMCGroupMessage> packets) {
        logger.info("RECV_GROUP_MESSAGE, MIMC handleGroupMessage Called.");
        try {
            groupMessages.addAll(packets);
            logger.info("RECV_GROUP_MESSAGE, GROUP_MESSAGES_SIZE:{}", groupMessages.size());
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
        }
    }

    public void handleServerAck(MIMCServerAck serverAck) {
        logger.info("RECV_SERVER_ACK, MIMC HandleServerAck Called. serverAck:{}, currentTimestamp:{}", serverAck, System.currentTimeMillis());
        try {
            Assert.assertTrue("Time interval too long", (System.currentTimeMillis() - serverAck.getTimestamp()) <= 3000);
            serverAcks.put(serverAck);
            logger.info("RECV_SERVER_ACK, SERVER_ACKS_SIZE:{}", serverAcks.size());
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
        }
    }


    public void handleSendMessageTimeout(MIMCMessage mimcMessage) {

    }

    public void handleSendGroupMessageTimeout(MIMCGroupMessage mimcGroupMessage) {

    }

    public void handleSendUnlimitedGroupMessageTimeout(MIMCGroupMessage groupMessage) {

    }

    public void handleCreateUnlimitedGroup(long topicId, String topicName, boolean success, String errMsg) {

    }

    public void handleJoinUnlimitedGroup(long topicId, int code, String message) {

    }

    public void handleQuitUnlimitedGroup(long topicId, int code, String message) {

    }

    public void handleUnlimitedGroupMessage(List<MIMCGroupMessage> packets) {
        logger.info("RECV_UNLIMITED_GROUP_MESSAGE, MIMC handleUnlimitedGroupMessage Called.");
        try {
            for (MIMCGroupMessage msg : packets) {
                logger.info("RECV_UNLIMITED_GROUP_MESSAGE, packetId:{}, topicId:{}", msg.getPacketId(), msg.getGroupId());
            }
            ucGroupMessages.addAll(packets);
            logger.info("RECV_UNLIMITED_GROUP_MESSAGE, MESSAGES_SIZE:{}", ucGroupMessages.size());
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
        }
    }

    public void handleDismissUnlimitedGroup(int code, String errMsg) {

    }

    public MIMCMessage pollMessage(long timeoutMs) {
        try {
            logger.info("Before POLL_MESSAGE, MESSAGE_SIZE:{}", messages.size());
            MIMCMessage msg = messages.poll(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("After POLL_MESSAGE, MESSAGE_SIZE:{}", messages.size());
            return msg;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public MIMCGroupMessage pollGroupMessage(long timeoutMs) {
        try {
            MIMCGroupMessage msg =  groupMessages.poll(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("POLL_GROUP_MESSAGE, GROUP_MESSAGE_SIZE:{}", messages.size());
            return msg;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public MIMCGroupMessage pollUCGroupMessage(long timeoutMs) {
        try {
            MIMCGroupMessage msg =  ucGroupMessages.poll(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("POLL_UNLIMITED_GROUP_MESSAGE, MESSAGE_SIZE:{}", messages.size());
            return msg;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public MIMCServerAck pollServerAck(long timeoutMs) {
        try {
            MIMCServerAck serverAck = serverAcks.poll(timeoutMs, TimeUnit.MILLISECONDS);
            logger.info("POLL_SERVER_ACK, SERVER_ACK_SIZE:{}", serverAcks.size());
            return serverAck;
        } catch (Exception e) {
            logger.warn("Exception:{}", e);
            return null;
        }
    }

    public int pollUCGroupMsgSize() {
        return ucGroupMessages.size();
    }

    public boolean clear() {
        messages.clear();
        groupMessages.clear();
        serverAcks.clear();
        ucGroupMessages.clear();
        return true;
    }
}