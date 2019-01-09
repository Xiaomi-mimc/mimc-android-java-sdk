package com.xiaomi.mimcdemo.common;

import android.content.Context;
import android.util.Base64;

import com.alibaba.fastjson.JSON;
import com.xiaomi.mimcdemo.R;
import com.xiaomi.mimcdemo.bean.Msg;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by houminjiang on 18-1-8.
 */

public class ParseJson {
    public static String parseCreateGroupJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("data");
            JSONObject topicInfo = object.getJSONObject("topicInfo");
            info += context.getString(R.string.group_id) + topicInfo.getString("topicId") + "\n";
            info += context.getString(R.string.group_name) + topicInfo.getString("topicName") + "\n";
            JSONArray members = object.getJSONArray("members");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.members) + member.getString("account")
                        + "    " + context.getString(R.string.uuid) + member.getString("uuid") + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseQueryGroupInfoJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("data");
            JSONObject topicInfo = object.getJSONObject("topicInfo");
            info += context.getString(R.string.group_id) + topicInfo.getString("topicId") + "\n";
            info += context.getString(R.string.group_name) + topicInfo.getString("topicName") + "\n";
            info += context.getString(R.string.group_bulletin) + topicInfo.getString("bulletin") + "\n";
            JSONArray members = object.getJSONArray("members");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.members) + member.getString("account")
                        + "    " + context.getString(R.string.uuid) + member.getString("uuid") + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseQueryGroupsOfAccountJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            JSONArray members = object.getJSONArray("data");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.group_id) + member.getString("topicId") + "\n";
                info += context.getString(R.string.group_name) + member.getString("topicName") + "\n";
                info += context.getString(R.string.group_bulletin) + member.getString("bulletin") + "\n";
                info += context.getString(R.string.uuid_owner_of_group) + member.getString("ownerUuid") + "\n";
                info += context.getString(R.string.name_owner_of_group) + member.getString("ownerAccount") + "\n\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseJoinGroupJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("data");
            JSONObject topicInfo = object.getJSONObject("topicInfo");
            info += context.getString(R.string.group_id) + topicInfo.getString("topicId") + "\n";
            info += context.getString(R.string.group_name) + topicInfo.getString("topicName") + "\n";
            JSONArray members = object.getJSONArray("members");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.members) + member.getString("account")
                        + "    " + context.getString(R.string.uuid) + member.getString("uuid") + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return info;
    }

    public static String parseQuitGroupJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            info = object.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseKickGroupJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("data");
            JSONObject topicInfo = object.getJSONObject("topicInfo");
            info += context.getString(R.string.group_id) + topicInfo.getString("topicId") + "\n";
            info += context.getString(R.string.group_name) + topicInfo.getString("topicName") + "\n";
            JSONArray members = object.getJSONArray("members");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.members) + member.getString("account")
                        + "    " + context.getString(R.string.uuid) + member.getString("uuid") + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseUpdateGroupJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("data");
            JSONObject topicInfo = object.getJSONObject("topicInfo");
            info += context.getString(R.string.group_id) + topicInfo.getString("topicId") + "\n";
            info += context.getString(R.string.uuid_owner_of_group) + topicInfo.getString("ownerUuid") + "\n";
            info += context.getString(R.string.group_name) + topicInfo.getString("topicName") + "\n";
            info += context.getString(R.string.group_bulletin) + topicInfo.getString("bulletin") + "\n";
            JSONArray members = object.getJSONArray("members");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.members) + member.getString("account")
                        + "    " + context.getString(R.string.uuid) + member.getString("uuid") + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseDismissGroupJson(final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            info = object.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseP2PHistoryJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("data");
            info += context.getString(R.string.pull_rows) + object.getInt("row") + "条\n\n";
            JSONArray members = object.getJSONArray("messages");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.to_account) + member.getString("toAccount") + "\n";
                info += context.getString(R.string.from_account) + member.getString("fromAccount") + "\n";
                info += context.getString(R.string.p2p_sequence) + member.getString("sequence") + "\n";
                String payload = new String(Base64.decode(member.getString("payload"), Base64.DEFAULT));
                Msg msg = null;
                try {
                    msg = JSON.parseObject(payload, Msg.class);
                } catch (Exception e) {
                }
                payload = (msg == null ? payload : new String(msg.getPayload()));
                info += context.getString(R.string.contents) + payload + "\n";
                info += context.getString(R.string.p2p_ts) + TimeUtils.utc2Local(member.getLong("ts")) + "\n\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseP2THistoryJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("data");
            info += context.getString(R.string.group_id) + object.getString("topicId") + "\n";
            info += context.getString(R.string.pull_rows) + object.getInt("row") + "条\n\n";
            JSONArray members = object.getJSONArray("messages");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.p2p_sequence) + member.getString("sequence") + "\n";
                info += context.getString(R.string.from_account) + member.getString("fromAccount") + "\n";
                String payload = new String(Base64.decode(member.getString("payload"), Base64.DEFAULT));
                Msg msg = null;
                try {
                    msg = JSON.parseObject(payload, Msg.class);
                } catch (Exception e) {
                }
                payload = (msg == null ? payload : new String(msg.getPayload()));
                info += context.getString(R.string.contents) + payload + "\n";
                info += context.getString(R.string.p2p_ts) + TimeUtils.utc2Local(member.getLong("ts")) + "\n\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseCreateUnlimitedGroupJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            if (object.getInt("code") != 200) {
                return json;
            }

            object = object.getJSONObject("data");

            info += context.getString(R.string.group_id) + object.getLong("topicId") + "\n";
            info += context.getString(R.string.group_name) + object.getString("topicName") + "\n";
            info += context.getString(R.string.uuid_owner_of_group) + object.getLong("ownerId") + "\n";
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseDismissUnlimitedGroupJson(final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            if (object.getInt("code") != 200) {
                return json;
            }
            info = object.getString("message") + "\n";
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseQueryUnlimitedGroupJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            if (object.getInt("code") != 200) {
                return json;
            }
            object = object.getJSONObject("data");
            JSONArray members = object.getJSONArray("members");
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = members.getJSONObject(i);
                info += context.getString(R.string.app_id) + member.getString("appId") + "\n";
                info += context.getString(R.string.uuid) + member.getString("uuid") + "\n";
                info += context.getString(R.string.account) + member.getString("appAccout") + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseQueryUnlimitedGroupsJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            if (object.getInt("code") != 200) {
                return json;
            }

            JSONArray members = object.getJSONArray("data");
            for (int i = 0; i < members.length(); i++) {
                // members.getLong(i)有可能结果不对
                info += context.getString(R.string.group_id) + members.getString(i) + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String parseQueryUnlimitedGroupOnlineUsersJson(Context context, final String json) {
        String info = "";
        if (json == null || json.isEmpty()) {
            return info;
        }

        try {
            JSONObject object = new JSONObject(json);
            if (object.getInt("code") != 200) {
                return json;
            }

            object = object.getJSONObject("data");
            info += context.getString(R.string.group_id) + object.getLong("topicId") + "\n";
            info += context.getString(R.string.members) + object.getInt("userCnt") + "\n";
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }
}
