package com.xiaomi.mimcdemo.performanceTest.utils;

import com.xiaomi.mimc.MIMCTokenFetcher;
import com.xiaomi.mimc.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MIMCCaseTokenFetcher implements MIMCTokenFetcher {
    private static final Logger logger = LoggerFactory.getLogger(MIMCCaseTokenFetcher.class);
    private String httpUrl;
    private String appId;
    private String appKey;
    private String appSecurt;
    private String appAccount;

    public MIMCCaseTokenFetcher(String appId, String appKey, String appSecurt, String httpUrl, String appAccount) {
        this.httpUrl = httpUrl;
        this.appId = appId;
        this.appKey = appKey;
        this.appSecurt = appSecurt;
        this.appAccount = appAccount;
    }

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
        obj.put("appSecret", appSecurt);
        obj.put("appAccount", appAccount);

        con.getOutputStream().write(obj.toString().getBytes("utf-8"));
        Assert.assertEquals(200, con.getResponseCode());

        String inputLine;
        StringBuffer content = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((inputLine = in.readLine()) != null) {
            content.append(StringUtils.trim(inputLine));
        }
        in.close();
        logger.info(content.toString());

        return content.toString();
    }
}