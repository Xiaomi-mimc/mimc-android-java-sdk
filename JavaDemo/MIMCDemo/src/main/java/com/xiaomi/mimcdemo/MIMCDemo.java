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
    private static final String appId = "2882303761517613988";
    private static final String appKey = "5361761377988";
    private static final String appSecurity = "2SZbrJOAL1xHRKb7L9AiRQ==";

    private final String appAccount1 = "leijun1986";
    private final String appAccount2 = "linbin1986";
    private MIMCUser leijun;
    private MIMCUser linbin;

    public MIMCDemo() throws Exception {
        leijun = MIMCUser.newInstance(appAccount1, "./files");
        linbin = MIMCUser.newInstance(appAccount2, "./files");
        init(leijun);
        init(linbin);
    }

    private void init(final MIMCUser MIMCUser) throws Exception {
        MIMCUser.registerTokenFetcher(new MIMCCaseTokenFetcher(appId, appKey, appSecurity, url, MIMCUser.getAppAccount()));
        MIMCUser.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
            public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                LOGGER.info("OnlineStatusHandler, Called, {}, isOnline:{}, errType:{}, :{}, errDesc:{}",
                        MIMCUser.getAppAccount(), status, errType, errReason, errDescription);
            }
        });
        MIMCUser.registerMessageHandler(new MIMCMessageHandler() {
            public void handleMessage(List<MIMCMessage> packets) {
                for (MIMCMessage p : packets) {
                    Msg msg = JSON.parseObject(new String(p.getPayload()), Msg.class);
                    if (msg.getMsgType() == Constant.TEXT) {
                        LOGGER.info("ReceiveMessage, P2P, {}-->{}, packetId:{}, payload:{}",
                                p.getFromAccount(), MIMCUser.getAppAccount(), p.getPacketId(), new String(msg.getContent()));
                    }
                }
            }

            public void handleGroupMessage(List<MIMCGroupMessage> packets) { /*TODO*/}

            public void handleUnlimitedGroupMessage(List<MIMCGroupMessage> list) {  /*TODO*/}

            public void handleServerAck(MIMCServerAck serverAck) {
                LOGGER.info("ReceiveMessageAck, serverAck:{}", serverAck);
            }

            public void handleSendMessageTimeout(MIMCMessage message) { /*TODO*/
                LOGGER.info("handleSendMessageTimeout, packetId:{}:{}", message.getPacketId());
            }

            public void handleSendGroupMessageTimeout(MIMCGroupMessage groupMessage) { /*TODO*/}

            public void handleSendUnlimitedGroupMessageTimeout(MIMCGroupMessage groupMessage) {  /*TODO*/}
        });
    }

    public void ready() throws Exception {
        leijun.login();
        linbin.login();

        Thread.sleep(2000);
    }

    public void sendMessage() throws Exception {
        if (!leijun.isOnline()) {
            LOGGER.error("{} login fail, quit!", leijun.getAppAccount());
            return;
        }
        if (!linbin.isOnline()) {
            LOGGER.error("{} login fail, quit!", linbin.getAppAccount());
            return;
        }

        Msg msg = new Msg();
        for (int i = 0; i < 1000; i++) {
            msg.setVersion(Constant.VERSION);
            msg.setMsgId(msg.getMsgId());
            msg.setMsgType(Constant.TEXT);
            msg.setTimestamp(System.currentTimeMillis());
            msg.setContent(String.format("leijun(%s)-->linbin(%s), %s", leijun.getUuid(), linbin.getUuid(), i).getBytes());

            String jsonStr = JSON.toJSONString(msg);
            leijun.sendMessage(linbin.getAppAccount(), jsonStr.getBytes());
            Thread.sleep(5000);
        }

        leijun.logout();
        linbin.logout();
        Thread.sleep(500);
        System.out.println("leijun status: " + leijun.isOnline());
        System.out.println("linbin status: " + linbin.isOnline());
        leijun.destroy();
        linbin.destroy();
    }

    public static void main(String[] args) throws Exception {
        MIMCDemo demo = new MIMCDemo();
        demo.ready();
        demo.sendMessage();

        System.exit(0);
    }

    public static class MIMCCaseTokenFetcher implements MIMCTokenFetcher {
        private String httpUrl;
        private String appId;
        private String appKey;
        private String appSecret;
        private String appAccount;

        public MIMCCaseTokenFetcher(String appId, String appKey, String appSecret, String httpUrl, String appAccount) {
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
         * @important: appId/appKey/appSecurity绝对不能如此用例一般存放于APP本地
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