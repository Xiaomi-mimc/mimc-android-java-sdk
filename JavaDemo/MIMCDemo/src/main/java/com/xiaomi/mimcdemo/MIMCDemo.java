package com.xiaomi.mimcdemo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaomi.mimc.*;
import com.xiaomi.mimc.common.MIMCConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MIMCDemo {
    private static Logger LOGGER = LoggerFactory.getLogger(MIMCDemo.class);

    /**
     * @Important: 以下appId/appKey/appSecurity是小米MIMCDemo APP所有，会不定期更新
     * 所以，开发者应该将以下三个值替换为开发者拥有APP的appId/appKey/appSecurity
     * @Important: 开发者访问小米开放平台(https : / / dev.mi.com / console / man /)，申请appId/appKey/appSecurity
     **/
    private static final String url = "https://mimc.chat.xiaomi.net/api/account/token";
    private static final long appId = 2882303761517669588L;
    private static final String appKey = "5111766983588";
    private static final String appSecurity = "b0L3IOz/9Ob809v8H2FbVg==";

    private final String appAccount1 = "User1";
    private final String appAccount2 = "User2";
    private MIMCUser user1;
    private MIMCUser user2;

    public MIMCDemo() throws Exception {
        user1 = MIMCUser.newInstance(appId, appAccount1, "./files");
        user2 = MIMCUser.newInstance(appId, appAccount2, "./files");
        init(user1);
        init(user2);
    }

    private void init(final MIMCUser mimcUser) throws Exception {
        mimcUser.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, url, mimcUser.getAppAccount()));
        mimcUser.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
            public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                LOGGER.info("OnlineStatusHandler, Called, {}, isOnline:{}, errType:{}, :{}, errDesc:{}",
                        mimcUser.getAppAccount(), status, errType, errReason, errDescription);
            }
        });
        mimcUser.registerMessageHandler(new MIMCMessageHandler() {
            public void handleMessage(List<MIMCMessage> packets) {
                for (MIMCMessage p : packets) {
                    try {
                        Msg msg = JSON.parseObject(new String(p.getPayload()), Msg.class);
                        LOGGER.info("ReceiveMessage, P2P, {}-->{}, packetId:{}, payload:{}",
                            p.getFromAccount(), p.getToAccount(), p.getPacketId(), new String(msg.getContent()));
                    } catch (Exception e) {
                        LOGGER.info("ReceiveMessage, P2P, {}-->{}, packetId:{}, payload:{}",
                            p.getFromAccount(), p.getToAccount(), p.getPacketId(), new String(p.getPayload()));
                    }
                }
            }

            public void handleGroupMessage(List<MIMCGroupMessage> packets) {}

            public void handleUnlimitedGroupMessage(List<MIMCGroupMessage> list) {}

            public void handleServerAck(MIMCServerAck serverAck) {
                LOGGER.info("ReceiveMessageAck, serverAck:{}", serverAck);
            }

            public void handleSendMessageTimeout(MIMCMessage message) {
                LOGGER.info("handleSendMessageTimeout, packetId:{}", message.getPacketId());
            }

            public void handleSendGroupMessageTimeout(MIMCGroupMessage groupMessage) {}

            public void handleSendUnlimitedGroupMessageTimeout(MIMCGroupMessage groupMessage) {}
        });
    }

    public void ready() throws Exception {
        user1.login();
        user2.login();
    }

    public void sendMessage() throws Exception {
        while (!user1.isOnline()) {
            Thread.sleep(200);
        }
        if (!user2.isOnline()) {
            Thread.sleep(200);
        }

        Msg msg = new Msg();
        for (int i = 0; i < 1000; i++) {
            msg.setVersion(Constant.VERSION);
            msg.setMsgId(msg.getMsgId());
            msg.setTimestamp(System.currentTimeMillis());
            msg.setContent(String.format("user1(%s)-->user2(%s), %s", user1.getUuid(), user2.getUuid(), i).getBytes());

            String jsonStr = JSON.toJSONString(msg);
            user1.sendMessage(user2.getAppAccount(), jsonStr.getBytes(), Constant.TEXT);
            Thread.sleep(5000);
        }

        user1.logout();
        user2.logout();
        Thread.sleep(500);
        System.out.println("user1 status: " + user1.isOnline());
        System.out.println("user2 status: " + user2.isOnline());
        user1.destroy();
        user2.destroy();
    }

    public static void main(String[] args) throws Exception {
        MIMCDemo demo = new MIMCDemo();
        demo.ready();
        demo.sendMessage();

        System.exit(0);
    }

    public static class MIMCCaseTokenFetcher implements MIMCTokenFetcher {
        private String httpUrl;
        private long appId;
        private String appKey;
        private String appSecret;
        private String appAccount;

        public MIMCCaseTokenFetcher(long appId, String appKey, String appSecret, String httpUrl, String appAccount) {
            this.httpUrl = httpUrl;
            this.appId = appId;
            this.appKey = appKey;
            this.appSecret = appSecret;
            this.appAccount = appAccount;
        }

        /**
         * @important: 此例中，fetchToken()直接上传(appId/appKey/appSecurity/appAccount)给小米TokenService，获取Token使用
         * 实际上，在生产环境中，fetchToken()应该只上传appAccount+password/cookies给AppProxyService，AppProxyService
         * 验证鉴权通过后，再上传(appId/appKey/appSecurity/appAccount)给小米TokenService，获取Token后返回给fetchToken()
         * @important: appKey/appSecurity绝对不能如此用例一般存放于APP本地
         **/
        public String fetchToken() throws Exception {
            URL url = new URL(httpUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.addRequestProperty("Content-Type", "application/json");

            JSONObject obj = new JSONObject();
            obj.put("appId", appId);
            obj.put("appKey", appKey);
            obj.put("appSecret", appSecret);
            obj.put("appAccount", appAccount);

            con.getOutputStream().write(obj.toString().getBytes("utf-8"));
            if (200 != con.getResponseCode()) {
                LOGGER.error("con.getResponseCode()!=200");
                System.exit(0);
            }

            String inputLine;
            StringBuffer content = new StringBuffer();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((inputLine = in.readLine()) != null) {
                content.append(trim(inputLine));
            }
            in.close();
            LOGGER.info(content.toString());

            return content.toString();
        }

        public String trim(String str) {
            return str == null ? null : str.trim();
        }
    }
}
