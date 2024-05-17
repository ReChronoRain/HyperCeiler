package com.sevtinge.hyperceiler.module.base.dexkit;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * dexkit 数据转为 JSON 储存
 */
public class DexKitData {
    private static final String TAG = "DexKitData";
    public static final String EMPTY = "";
    public static final ArrayList<String> EMPTYLIST = new ArrayList<>();
    // public String label;
    private String tag;
    private String type;
    private String clazz;
    private String method;
    private ArrayList<String> param;
    private String field;

    public DexKitData(String tag, String type, String clazz,
                      String method, ArrayList<String> param,
                      String field) {
        // label = l;
        this.tag = tag;
        this.type = type;
        this.clazz = clazz;
        this.method = method;
        this.param = param;
        this.field = field;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            // jsonObject.put("l", label);
            jsonObject.put("tag", tag);
            jsonObject.put("type", type);
            jsonObject.put("clazz", clazz);
            jsonObject.put("method", method);
            jsonObject.put("param", param);
            jsonObject.put("field", field);
            return jsonObject;
        } catch (JSONException e) {
            logE(TAG, "failed to convert JSON: " + e);
        }
        return jsonObject;
    }

    public static String getTAG(JSONObject jsonObject) {
        try {
            return jsonObject.getString("tag");
        } catch (JSONException e) {
            logE(TAG, "failed to get tag!" + e);
        }
        return "null";
    }

    public static String getType(JSONObject jsonObject) {
        try {
            return jsonObject.getString("type");
        } catch (JSONException e) {
            logE(TAG, "failed to get type!" + e);
        }
        return "null";
    }

    public static String getClazz(JSONObject jsonObject) {
        try {
            return jsonObject.getString("clazz");
        } catch (JSONException e) {
            logE(TAG, "failed to get class list: " + e);
        }
        return null;
    }

    public static String getMethod(JSONObject jsonObject) {
        try {
            return jsonObject.getString("method");
        } catch (JSONException e) {
            logE(TAG, "failed to get method!" + e);
        }
        return null;
    }

    public static ArrayList<String> getParam(JSONObject jsonObject) {
        try {
            return stringToArrays(jsonObject.getString("param"));
        } catch (JSONException e) {
            logE(TAG, "failed to get param!" + e);
        }
        return new ArrayList<>();
    }

    public static String getFiled(JSONObject jsonObject) {
        try {
            return jsonObject.getString("field");
        } catch (JSONException e) {
            logE(TAG, "failed to get field!" + e);
        }
        return null;
    }

    public static ArrayList<String> stringToArrays(String s) {
        s = s.replace("[", "").replace("]", "").replace(" ", "");
        return new ArrayList<>(Arrays.asList(s.split(",")));
    }

    public static ArrayList<JSONObject> toArray(String json) {
        try {
            if (json == null) return new ArrayList<>();
            if (json.isEmpty()) return new ArrayList<>();
            if ("[]".equals(json)) return new ArrayList<>();
            ArrayList<JSONObject> list = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(obj);
            }
            return list;
        } catch (Exception e) {
            logE(TAG, "Failed to convert Array!" + e);
        }
        return new ArrayList<>();
    }
}
