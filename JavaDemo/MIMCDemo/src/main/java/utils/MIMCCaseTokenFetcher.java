package utils;

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
    private long appId;
    private String appKey;
    private String appSecurity;
    private String appAccount;

    public MIMCCaseTokenFetcher(long appId, String appKey, String appSecurity, String httpUrl, String appAccount) {
        this.httpUrl = httpUrl;
        this.appId = appId;
        this.appKey = appKey;
        this.appSecurity = appSecurity;
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
        obj.put("appSecret", appSecurity);
        obj.put("appAccount", appAccount);

        con.getOutputStream().write(obj.toString().getBytes("utf-8"));

        long t0 = System.currentTimeMillis();
        Assert.assertEquals(200, con.getResponseCode());
        long t1 = System.currentTimeMillis();

        if (t1 - t0 > 2000) {
            logger.warn("\n{}\nFETCH TOKEN MORE THAN 2000 ms: {} - {} = {}", url, t1, t0, t1 - t0);
        }
        Assert.assertTrue("FETCH TOKEN MORE THAN 2000 ms: " + String.valueOf(t1 -t0), t1 - t0 < 2001);

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