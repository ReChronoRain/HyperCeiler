package com.sevtinge.hyperceiler.safe;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 崩溃记录数据库
 */
class CrashRecord {
    public static final String TAG = ITAG.TAG + ": CrashRecord";
    // public String label;
    public String pkg;
    public long time;
    public int count;

    public CrashRecord(String p, long t, int c) {
        // label = l;
        pkg = p;
        time = t;
        count = c;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            // jsonObject.put("l", label);
            jsonObject.put("p", pkg);
            jsonObject.put("t", time);
            jsonObject.put("c", count);
            return jsonObject;
        } catch (JSONException e) {
            XposedLogUtils.logE(TAG, "Failed to convert JSON!" + e);
        }
        return jsonObject;
    }

    public JSONObject toJSONSmall() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("p", pkg);
            jsonObject.put("c", count);
            return jsonObject;
        } catch (JSONException e) {
            XposedLogUtils.logE(TAG, "Failed to convert JSON!" + e);
        }
        return jsonObject;
    }

    /*public static String getLabel(JSONObject jsonObject) {
        try {
            return jsonObject.getString("l");
        } catch (JSONException e) {
            logE(TAG, "Failed to get name!" + e);
        }
        return "null";
    }*/

    public static String getPkg(JSONObject jsonObject) {
        try {
            return jsonObject.getString("p");
        } catch (JSONException e) {
            XposedLogUtils.logE(TAG, "Failed to get package name!" + e);
        }
        return "null";
    }

    public static long getTime(JSONObject jsonObject) {
        try {
            return jsonObject.getLong("t");
        } catch (JSONException e) {
            XposedLogUtils.logE(TAG, "Failed to get timestamp!" + e);
        }
        return -1L;
    }

    public static int getCount(JSONObject jsonObject) {
        try {
            return jsonObject.getInt("c");
        } catch (JSONException e) {
            XposedLogUtils.logE(TAG, "Failed to get the number of times!" + e);
        }
        return -1;
    }

    public static JSONObject putParam(JSONObject jsonObject, long time, int count) {
        try {
            jsonObject.put("c", count);
            jsonObject.put("t", time);
            return jsonObject;
        } catch (JSONException e) {
            XposedLogUtils.logE(TAG, "Failed to update data!" + e);
        }
        return null;
    }

    public static ArrayList<JSONObject> toArray(String json) {
        try {
            ArrayList<JSONObject> list = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(obj);
            }
            return list;
        } catch (Exception e) {
            XposedLogUtils.logE(TAG, "Failed to convert Array!" + e);
        }
        return new ArrayList<>();
    }
}
