package com.xiaomi.mimcdemo.common;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.xiaomi.mimc.MIMCGroupMessage;
import com.xiaomi.mimc.MIMCMessage;
import com.xiaomi.mimc.MIMCMessageHandler;
import com.xiaomi.mimc.MIMCOnlineStatusListener;
import com.xiaomi.mimc.MIMCRtsCallHandler;
import com.xiaomi.mimc.MIMCServerAck;
import com.xiaomi.mimc.MIMCTokenFetcher;
import com.xiaomi.mimc.MIMCUnlimitedGroupHandler;
import com.xiaomi.mimc.MIMCUser;
import com.xiaomi.mimc.common.MIMCConstant;
import com.xiaomi.mimc.data.LaunchedResponse;
import com.xiaomi.mimc.data.RtsChannelType;
import com.xiaomi.mimc.data.RtsDataType;
import com.xiaomi.mimcdemo.bean.ChatMsg;
import com.xiaomi.mimcdemo.bean.Msg;
import com.xiaomi.mimcdemo.listener.OnCallStateListener;
import com.xiaomi.mimcdemo.ui.MIMCApplication;
import com.xiaomi.mimcdemo.ui.VoiceCallActivity;
import com.xiaomi.msg.data.XMDPacket;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserManager {
    /**
     * appId/appKey/appSecret，小米开放平台(https://dev.mi.com/console/appservice/mimc.html)申请
     * 其中appKey和appSecret不可存储于APP端，应存储于APP自己的服务器，以防泄漏。
     *
     * 此处appId/appKey/appSec为小米MimcDemo所有，会在一定时间后失效，建议开发者自行申请
     **/
    // online
    private long appId = 2882303761517669588L;
    private String appKey = "5111766983588";
    private String appSecret = "b0L3IOz/9Ob809v8H2FbVg==";
    private String regionKey = "REGION_CN";
    private String domain = "https://mimc.chat.xiaomi.net/";

    // staging
//    private long appId = 2882303761517479657L;
//    private String appKey = "5221747911657";
//    private String appSecret = "PtfBeZyC+H8SIM/UXhZx1w==";
//    private String regionKey = "REGION_CN";
//    private String domain = "http://10.38.162.149/";

    // 用户登录APP的帐号
    private String url;
    private MIMCUser mimcUser;
    private MIMCConstant.OnlineStatus mStatus;
    private final static UserManager instance = new UserManager();
    private OnHandleMIMCMsgListener onHandleMIMCMsgListener;
    private OnCallStateListener onCallStateListener;
    public static int TIMEOUT_ON_LAUNCHED = 1 * 30 * 1000;
    public static int STATE_TIMEOUT = 0;
    public static int STATE_AGREE = 1;
    public static int STATE_REJECT = 2;
    public static int STATE_INTERRUPT = 3;
    private volatile int answer = STATE_TIMEOUT;
    private Object lock = new Object();
    private static final String TAG = "UserManager";

    // 设置消息监听
    public void setHandleMIMCMsgListener(OnHandleMIMCMsgListener onHandleMIMCMsgListener) {
        this.onHandleMIMCMsgListener = onHandleMIMCMsgListener;
    }

    public void setCallStateListener(OnCallStateListener onCallStateListener) {
        this.onCallStateListener = onCallStateListener;
    }

    public interface OnHandleMIMCMsgListener {
        void onHandleMessage(ChatMsg chatMsg);
        void onHandleGroupMessage(ChatMsg chatMsg);
        void onHandleStatusChanged(MIMCConstant.OnlineStatus status);
        void onHandleServerAck(MIMCServerAck serverAck);
        void onHandleCreateGroup(String json, boolean isSuccess);
        void onHandleQueryGroupInfo(String json, boolean isSuccess);
        void onHandleQueryGroupsOfAccount(String json, boolean isSuccess);
        void onHandleJoinGroup(String json, boolean isSuccess);
        void onHandleQuitGroup(String json, boolean isSuccess);
        void onHandleKickGroup(String json, boolean isSuccess);
        void onHandleUpdateGroup(String json, boolean isSuccess);
        void onHandleDismissGroup(String json, boolean isSuccess);
        void onHandlePullP2PHistory(String json, boolean isSuccess);
        void onHandlePullP2THistory(String json, boolean isSuccess);
        void onHandleSendMessageTimeout(MIMCMessage message);
        void onHandleSendGroupMessageTimeout(MIMCGroupMessage groupMessage);
        void onHandleJoinUnlimitedGroup(long topicId, int code, String errMsg);
        void onHandleQuitUnlimitedGroup(long topicId, int code, String errMsg);
        void onHandleDismissUnlimitedGroup(String json, boolean isSuccess);
        void onHandleQueryUnlimitedGroupMembers(String json, boolean isSuccess);
        void onHandleQueryUnlimitedGroups(String json, boolean isSuccess);
        void onHandleQueryUnlimitedGroupOnlineUsers(String json, boolean isSuccess);
    }

    public static UserManager getInstance() {
        return instance;
    }

    /**
     * 获取用户帐号
     * @return 成功返回用户帐号，失败返回""
     */
    public String getAccount() {
        return getMIMCUser() != null ? getMIMCUser().getAppAccount() : "";
    }

    /**
     * 获取用户在线状态
     * @return STATUS_LOGIN_SUCCESS 在线，STATUS_LOGOUT 下线，STATUS_LOGIN_FAIL 登录失败
     */
    public MIMCConstant.OnlineStatus getStatus() {
        return mStatus;
    }

    public void addMsg(ChatMsg chatMsg) {
        onHandleMIMCMsgListener.onHandleMessage(chatMsg);
    }

    public void addGroupMsg(ChatMsg chatMsg) {
        onHandleMIMCMsgListener.onHandleGroupMessage(chatMsg);
    }

    public void sendMsg(String toAppAccount, byte[] payload, String bizType) {
        Msg msg = new Msg();
        msg.setVersion(Constant.VERSION);
        msg.setMsgId(msg.getMsgId());
        msg.setTimestamp(System.currentTimeMillis());
        msg.setPayload(payload);
        String json = JSON.toJSONString(msg);
        mimcUser.sendMessage(toAppAccount, json.getBytes(), bizType);
        if (bizType.equals(Constant.TEXT) || bizType.equals(Constant.PIC_FILE)) {
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setFromAccount(mimcUser.getAppAccount());
            chatMsg.setMsg(msg);
            chatMsg.setSingle(true);
            addMsg(chatMsg);
        }
    }

    public void sendGroupMsg(long groupID, byte[] content, String bizType, boolean isUnlimitedGroup) {
        Msg msg = new Msg();
        msg.setVersion(Constant.VERSION);
        msg.setMsgId(msg.getMsgId());
        msg.setTimestamp(System.currentTimeMillis());
        msg.setPayload(content);
        String json = JSON.toJSONString(msg);
        if (isUnlimitedGroup) {
            mimcUser.sendUnlimitedGroupMessage(groupID, json.getBytes(), bizType);
        } else {
            mimcUser.sendGroupMessage(groupID, json.getBytes(), bizType);
        }
        if (bizType.equals(Constant.TEXT) || bizType.equals(Constant.PIC_FILE)) {
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setFromAccount(mimcUser.getAppAccount());
            chatMsg.setMsg(msg);
            chatMsg.setSingle(false);
            addMsg(chatMsg);
        }
    }

    /**
     * 获取用户
     * @return  返回已创建用户
     */
    public MIMCUser getMIMCUser() {
        return mimcUser;
    }

    /**
     * 创建用户
     * @param appAccount APP自己维护的用户帐号，不能为null
     * @return 返回新创建的用户
     */
    public MIMCUser newMIMCUser(String appAccount){
        if (appAccount == null || appAccount.isEmpty()) return null;

        // 若是新用户，先释放老用户资源
        if (mimcUser != null) {
            mimcUser.logout();
            mimcUser.destroy();
        }

        // online
        // cachePath必须传入，用于缓存文件读写，否则返回null
        mimcUser = MIMCUser.newInstance(appId, appAccount, MIMCApplication.getContext().getExternalCacheDir().getPath(), MIMCApplication.getContext().getCacheDir().getPath());
        // staging
//        mimcUser = MIMCUser.newInstance(appAccount, MIMCApplication.getContext().getExternalCacheDir().getPath(), MIMCApplication.getContext().getCacheDir().getPath(), "http://10.38.162.117:6000/gslb/", "http://10.38.162.149/");
        // 注册相关监听，必须
        mimcUser.registerTokenFetcher(new TokenFetcher());
        mimcUser.registerMessageHandler(new MessageHandler());
        mimcUser.registerOnlineStatusListener(new OnlineStatusListener());
        mimcUser.registerRtsCallHandler(new RTSHandler());
        mimcUser.registerUnlimitedGroupHandler(new UnlimitedGroupHandler());

        return mimcUser;
    }

    class UnlimitedGroupHandler implements MIMCUnlimitedGroupHandler {
        @Override
        public void handleCreateUnlimitedGroup(long topicId, String topicName, int code, String desc, Object obj) {
            Log.i(TAG, String.format("handleCreateUnlimitedGroup topicId:%d topicName:%s code:%d errMsg:%s"
                , topicId, topicName, code, desc));
        }

        @Override
        public void handleJoinUnlimitedGroup(long topicId, int code, String errMsg, Object obj) {
            onHandleMIMCMsgListener.onHandleJoinUnlimitedGroup(topicId, code, errMsg);
        }

        @Override
        public void handleQuitUnlimitedGroup(long topicId, int code, String errMsg, Object obj) {
            onHandleMIMCMsgListener.onHandleQuitUnlimitedGroup(topicId, code, errMsg);
        }

        @Override
        public void handleDismissUnlimitedGroup(long topicId, int code, String errMsg, Object obj) {
            onHandleMIMCMsgListener.onHandleDismissUnlimitedGroup(errMsg, false);
        }

        @Override
        public void handleDismissUnlimitedGroup(long topicId) {

        }
    }

    class RTSHandler implements MIMCRtsCallHandler {
        @Override
        public LaunchedResponse onLaunched(String fromAccount, String fromResource, long callId, byte[] appContent) {
            synchronized(lock) {
                Log.i(TAG, String.format("新会话请求来了 callId:%d", callId));
                String callType = new String(appContent);
                if (callType.equalsIgnoreCase("AUDIO")) {
                    VoiceCallActivity.actionStartActivity(MIMCApplication.getContext(), fromAccount, callId);
                } else if (callType.equalsIgnoreCase("VIDEO")) {

                }

                try {
                    answer = STATE_TIMEOUT;
                    lock.wait(TIMEOUT_ON_LAUNCHED);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Call lock exception:", e);
                    answer = STATE_INTERRUPT;
                }

                boolean isAgree = false;
                String msg = "timeout";
                if (answer == STATE_TIMEOUT) {
                    if (onCallStateListener != null) onCallStateListener.onClosed(callId, msg);
                } else if (answer == STATE_AGREE) {
                    isAgree = true;
                    msg = "agreed";
                } else if (answer == STATE_REJECT) {
                    msg = "rejected";
                    if (onCallStateListener != null) {
                        onCallStateListener.onClosed(callId, msg);
                    }
                } else if (answer == STATE_INTERRUPT) {
                    msg = "interrupted";
                    if (onCallStateListener != null) {
                        onCallStateListener.onClosed(callId, msg);
                    }
                }

                return new LaunchedResponse(isAgree, msg);
            }
        }

        @Override
        public void onAnswered(long callId, boolean accepted, String errMsg) {
            Log.i(TAG, "会话接通 callId:" + callId + " accepted:" + accepted + " errMsg:" + errMsg);
            if (onCallStateListener != null) onCallStateListener.onAnswered(callId, accepted, errMsg);
        }

        @Override
        public void onData(long callId, String fromAccount, String resource, byte[] data, RtsDataType dataType, RtsChannelType channelType) {
            onCallStateListener.handleData(callId, dataType, data);
        }

        @Override
        public void onClosed(long callId, String errMsg) {
            Log.i(TAG, "会话关闭 callId:" + callId + " errMsg:" + errMsg);
            if (onCallStateListener != null) onCallStateListener.onClosed(callId, errMsg);
        }

        @Override
        public void onSendDataSuccess(long callId, int dataId, Object context) {

        }

        @Override
        public void onSendDataFailure(long callId, int dataId, Object context) {

        }
    }

    public void answerCall() {
        synchronized (lock) {
            answer = STATE_AGREE;
            lock.notify();
        }
    }

    public void rejectCall() {
        synchronized (lock) {
            answer = STATE_REJECT;
            lock.notify();
        }
    }

    class OnlineStatusListener implements MIMCOnlineStatusListener {
        @Override
        public void statusChange(MIMCConstant.OnlineStatus status, String type, String reason, String desc) {
            mStatus = status;
            onHandleMIMCMsgListener.onHandleStatusChanged(status);
            Log.d(TAG, String.format("statusChange status:%s errType:%s errReason:%s errDescription:%s", status, type, reason, desc));
        }
    }

    class MessageHandler implements MIMCMessageHandler {
        /**
         * 接收单聊消息
         * MIMCMessage类
         * String packetId 消息ID
         * long sequence 序列号
         * String fromAccount 发送方帐号
         * String toAccount 接收方帐号
         * byte[] payload 消息体
         * long timestamp 时间戳
         */
        @Override
        public void handleMessage(List<MIMCMessage> packets) {
            for (int i = 0; i < packets.size(); ++i) {
                MIMCMessage mimcMessage = packets.get(i);
                try {
                    Msg msg = JSON.parseObject(new String(mimcMessage.getPayload()), Msg.class);
                    ChatMsg chatMsg = new ChatMsg();
                    chatMsg.setBizType(mimcMessage.getBizType());
                    chatMsg.setFromAccount(mimcMessage.getFromAccount());
                    chatMsg.setMsg(msg);
                    chatMsg.setSingle(true);
                    addMsg(chatMsg);
                } catch (Exception e) {
                    Msg msg = new Msg();
                    msg.setTimestamp(System.currentTimeMillis());
                    msg.setPayload(mimcMessage.getPayload());
                    ChatMsg chatMsg = new ChatMsg();
                    chatMsg.setBizType(mimcMessage.getBizType());
                    chatMsg.setFromAccount(mimcMessage.getFromAccount());
                    chatMsg.setMsg(msg);
                    chatMsg.setSingle(true);
                    addMsg(chatMsg);
                }
            }
        }

        /**
         * 接收群聊消息
         * MIMCGroupMessage类
         * String packetId 消息ID
         * long groupId 群ID
         * long sequence 序列号
         * String fromAccount 发送方帐号
         * byte[] payload 消息体
         * long timestamp 时间戳
         */
        @Override
        public void handleGroupMessage(List<MIMCGroupMessage> packets) {
            for (int i = 0; i < packets.size(); i++) {
                MIMCGroupMessage mimcGroupMessage = packets.get(i);
                try {
                    Msg msg = JSON.parseObject(new String(packets.get(i).getPayload()), Msg.class);
                    ChatMsg chatMsg = new ChatMsg();
                    chatMsg.setBizType(mimcGroupMessage.getBizType());
                    chatMsg.setFromAccount(mimcGroupMessage.getFromAccount());
                    chatMsg.setMsg(msg);
                    chatMsg.setSingle(false);
                    addGroupMsg(chatMsg);
                } catch (Exception e) {
                    Msg msg = new Msg();
                    msg.setTimestamp(System.currentTimeMillis());
                    msg.setPayload(packets.get(i).getPayload());
                    ChatMsg chatMsg = new ChatMsg();
                    chatMsg.setBizType(mimcGroupMessage.getBizType());
                    chatMsg.setFromAccount(mimcGroupMessage.getFromAccount());
                    chatMsg.setMsg(msg);
                    chatMsg.setSingle(false);
                    addGroupMsg(chatMsg);
                }
            }
        }

        /**
         * 接收服务端已收到发送消息确认
         * MIMCServerAck类
         * String packetId 消息ID
         * long sequence 序列号
         * long timestamp 时间戳
         */
        @Override
        public void handleServerAck(MIMCServerAck serverAck) {
            onHandleMIMCMsgListener.onHandleServerAck(serverAck);
        }

        /**
         * 接收单聊超时消息
         * @param message 单聊消息类
         */
        @Override
        public void handleSendMessageTimeout(MIMCMessage message) {
            onHandleMIMCMsgListener.onHandleSendMessageTimeout(message);
        }

        /**
         *接收发送群聊超时消息
         * @param groupMessage 群聊消息类
         */
        @Override
        public void handleSendGroupMessageTimeout(MIMCGroupMessage groupMessage) {
            onHandleMIMCMsgListener.onHandleSendGroupMessageTimeout(groupMessage);
        }

        @Override
        public void handleSendUnlimitedGroupMessageTimeout(MIMCGroupMessage mimcGroupMessage) {

        }

        @Override
        public void handleUnlimitedGroupMessage(List<MIMCGroupMessage> packets) {
            for (int i = 0; i < packets.size(); i++) {
                MIMCGroupMessage mimcGroupMessage = packets.get(i);
                try {
                    Msg msg = JSON.parseObject(new String(packets.get(i).getPayload()), Msg.class);
                    ChatMsg chatMsg = new ChatMsg();
                    chatMsg.setBizType(mimcGroupMessage.getBizType());
                    chatMsg.setFromAccount(mimcGroupMessage.getFromAccount());
                    chatMsg.setMsg(msg);
                    chatMsg.setSingle(false);
                    addGroupMsg(chatMsg);
                } catch (Exception e) {
                    Msg msg = new Msg();
                    msg.setTimestamp(System.currentTimeMillis());
                    msg.setPayload(packets.get(i).getPayload());
                    ChatMsg chatMsg = new ChatMsg();
                    chatMsg.setBizType(mimcGroupMessage.getBizType());
                    chatMsg.setFromAccount(mimcGroupMessage.getFromAccount());
                    chatMsg.setMsg(msg);
                    chatMsg.setSingle(false);
                    addGroupMsg(chatMsg);
                }
            }
        }
    }

    class TokenFetcher implements MIMCTokenFetcher {
        @Override
        public String fetchToken() {
            /**
             * fetchToken()由SDK内部线程调用，获取小米Token服务器返回的JSON字符串
             * 本MimcDemo直接从小米Token服务器获取JSON串，只解析出键data对应的值返回即可，切记！！！
             * 强烈建议，APP从自己服务器获取data对应的JSON串，APP自己的服务器再从小米Token服务器获取，以防appKey和appSecret泄漏
             */

            url = domain + "api/account/token";
            String appAccount = getAccount();
            String json = "{\"appId\":" + appId + ",\"appKey\":\"" + appKey + "\",\"appSecret\":\"" +
                appSecret + "\",\"appAccount\":\"" + appAccount + "\",\"regionKey\":\"" + regionKey + "\"}";
            MediaType JSON = MediaType.parse("application/json;charset=utf-8");
            OkHttpClient client = new OkHttpClient();
            Request request = new Request
                .Builder()
                .url(url)
                .post(RequestBody.create(JSON, json))
                .build();
            Call call = client.newCall(request);
            JSONObject data = null;
            try {
                Response response = call.execute();
                data = new JSONObject(response.body().string());
                int code = data.getInt("code");
                if (code != 200) {
                    //logger.warn("Error, code = " + code);
                    return null;
                }
            } catch (Exception e) {
                //logger.warn("Get token exception: " + e);
            }

            return data != null ? data.toString() : null;
        }
    }

    public long dialCall(String toAppAccount, String toResource, byte[] data) {
        if (getMIMCUser() != null) {
            return getMIMCUser().dialCall(toAppAccount, toResource, data);
        }

        return -1;
    }

    public void closeCall(long callId) {
        if (getMIMCUser() != null) {
            getMIMCUser().closeCall(callId);
        }
    }

    public int sendRTSData(long callId, byte[] data, RtsDataType dataType) {
        if (getMIMCUser() != null) {
            return getMIMCUser().sendRtsData(callId, data, dataType, XMDPacket.DataPriority.P0, true, 0, RtsChannelType.RELAY, null);
        }

        return -1;
    }

    /**
     * 创建群
     * @param groupName 群名
     * @param users 群成员，多个成员之间用英文逗号(,)分隔
     */
    public void createGroup(final String groupName, final String users) {
        url = domain + "api/topic/" + appId;
        String json = "{\"topicName\":\"" + groupName + "\", \"accounts\":\"" + users + "\"}";
        MediaType JSON = MediaType.parse("application/json");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .post(RequestBody.create(JSON, json))
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleCreateGroup(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询指定群信息
     * @param groupId 群ID
     */
    public void queryGroupInfo(final String groupId) {
        url = domain + "api/topic/" + appId + "/" + groupId;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .get()
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleQueryGroupInfo(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询所属群信息
     */
    public void queryGroupsOfAccount() {
        url = domain + "api/topic/" + appId + "/account";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .get()
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleQueryGroupsOfAccount(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 邀请用户加入群
     * @param groupId 群ID
     * @param users 加入成员，多个成员之间用英文逗号(,)分隔
     */
    public void joinGroup(final String groupId, final String users) {
        url = domain + "api/topic/" + appId + "/" + groupId + "/accounts";
        String json = "{\"accounts\":\"" + users + "\"}";
        MediaType JSON = MediaType.parse("application/json");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .post(RequestBody.create(JSON, json))
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleJoinGroup(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 非群主成员退群
     * @param groupId 群ID
     */
    public void quitGroup(final String groupId) {
        url = domain + "api/topic/" + appId + "/" + groupId + "/account";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .delete()
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleQuitGroup(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 群主踢成员出群
     * @param groupId 群ID
     * @param users 群成员，多个成员之间用英文逗号(,)分隔
     */
    public void kickGroup(final String groupId, final String users) {
        url = domain + "api/topic/" + appId + "/" + groupId + "/accounts?accounts=" + users;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .delete()
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleKickGroup(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 群主更新群信息
     * @param groupId 群ID
     * @param newOwnerAccount 若为群成员则指派新的群主
     * @param newGroupName 群名
     * @param newGroupBulletin 群公告
     */
    public void updateGroup(final String groupId, final String newOwnerAccount,  final String newGroupName, final String newGroupBulletin) {
        url = domain + "api/topic/" + appId + "/" + groupId;
        // 注意：不指定的信息则不更新（键值对一起不指定）
        String json = "{";
        if (!newOwnerAccount.isEmpty()) {
            json += "\"ownerAccount\":\"" + newOwnerAccount + "\"";
        } if (!newGroupName.isEmpty()) {
            json += "\"topicName\":\""+ newGroupName + "\"";
        } if (!newGroupBulletin.isEmpty()) {
            json += "\"bulletin\":\""+ newGroupBulletin + "\"";
        }
        json += "}";
        MediaType JSON = MediaType.parse("application/json");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .put(RequestBody.create(JSON, json))
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleUpdateGroup(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *群主销毁群
     * @param groupId 群ID
     */
    public void dismissGroup(final String groupId) {
        url = domain + "api/topic/" + appId + "/" + groupId;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("token", mimcUser.getToken())
                .delete()
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleCreateGroup(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandleDismissGroup(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拉取单聊消息记录
     * @param toAccount 接收方帐号
     * @param fromAccount 发送方帐号
     * @param utcFromTime 开始时间
     * @param utcToTime 结束时间
     * 注意：utcFromTime和utcToTime的时间间隔不能超过24小时，查询状态为[utcFromTime,utcToTime)，单位毫秒，UTC时间
     */
    public void pullP2PHistory(String toAccount, String fromAccount, String utcFromTime, String utcToTime) {
        url = domain + "api/msg/p2p/query/";
        String json = "{\"toAccount\":\"" + toAccount + "\", \"fromAccount\":\""
                + fromAccount + "\", \"utcFromTime\":\"" + utcFromTime + "\", \"utcToTime\":\"" +
                utcToTime + "\"}";
        MediaType JSON = MediaType.parse("application/json;charset=UTF-8");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("Accept", "application/json;charset=UTF-8")
                .addHeader("token", mimcUser.getToken())
                .post(RequestBody.create(JSON, json))
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandlePullP2PHistory(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandlePullP2PHistory(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拉取群聊消息记录
     * @param account 拉取者帐号
     * @param topicId 群ID
     * @param utcFromTime 开始时间
     * @param utcToTime 结束时间
     * 注意：utcFromTime和utcToTime的时间间隔不能超过24小时，查询状态为[utcFromTime,utcToTime)，单位毫秒，UTC时间
     */
    public void pullP2THistory(String account, String topicId, String utcFromTime, String utcToTime) {
        url = domain + "api/msg/p2t/query/";
        String json = "{\"account\":\"" + account + "\", \"topicId\":\""
                + topicId + "\", \"utcFromTime\":\"" + utcFromTime + "\", \"utcToTime\":\"" + utcToTime + "\"}";
        MediaType JSON = MediaType.parse("application/json;charset=UTF-8");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("Accept", "application/json;charset=UTF-8")
                .addHeader("token", mimcUser.getToken())
                .post(RequestBody.create(JSON, json))
                .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandlePullP2THistory(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        onHandleMIMCMsgListener.onHandlePullP2THistory(response.body().string(), true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询无限大群成员
     * @param topicId 群ID
     */
    public void queryUnlimitedGroupMembers(long topicId) {
        url = domain + "/api/uctopic/userlist";
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request
            .Builder()
            .url(url)
            .addHeader("token", mimcUser.getToken())
            .addHeader("topicId", String.valueOf(topicId))
            .get()
            .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleQueryUnlimitedGroupMembers(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        onHandleMIMCMsgListener.onHandleQueryUnlimitedGroupMembers(json, true);
                    } else {
                        onHandleMIMCMsgListener.onHandleQueryUnlimitedGroupMembers(response.message(), false);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询无限大群
     */
    public void queryUnlimitedGroups() {
        String url = domain + "/api/uctopic/topics";
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request
            .Builder()
            .url(url)
            .addHeader("token", mimcUser.getToken())
            .get()
            .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleQueryUnlimitedGroups(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        onHandleMIMCMsgListener.onHandleQueryUnlimitedGroups(json, true);
                    } else {
                        onHandleMIMCMsgListener.onHandleQueryUnlimitedGroups(response.message(), false);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询无限大群在线用户数
     * @param topicId
     */
    public void queryUnlimitedGroupOnlineUsers(long topicId) {
        url = domain + "/api/uctopic/onlineinfo";
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request
            .Builder()
            .url(url)
            .addHeader("token", mimcUser.getToken())
            .addHeader("topicId", String.valueOf(topicId))
            .get()
            .build();
        try {
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onHandleMIMCMsgListener.onHandleQueryUnlimitedGroupOnlineUsers(e.getMessage(), false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        onHandleMIMCMsgListener.onHandleQueryUnlimitedGroupOnlineUsers(json, true);
                    } else {
                        onHandleMIMCMsgListener.onHandleQueryUnlimitedGroupOnlineUsers(response.message(), false);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}