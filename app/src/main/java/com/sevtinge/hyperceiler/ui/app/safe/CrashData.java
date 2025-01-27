/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.app.safe;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.PropUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CrashData {
    private static final String TAG = ITAG.TAG + ": CrashRecord";
    private static final HashMap<String, String> scopeMap = new HashMap<>();
    private static final HashMap<String, String> swappedMap = new HashMap<>();

    /**
     * 把模块当前使用的全部作用域加入 Map。
     *
     * @return Map
     * @noinspection SameReturnValue
     */
    public static HashMap<String, String> scopeData() {
        if (scopeMap.isEmpty()) {
            scopeMap.put("com.android.systemui", "systemui");
            scopeMap.put("com.android.settings", "settings");
            scopeMap.put("com.miui.home", "home");
            scopeMap.put("com.miui.securitycenter", "center");
            scopeMap.put("com.hchen.demo", "demo");
            /*scopeMap.put("android", "android");
            scopeMap.put("com.android.browser", "browser");
            scopeMap.put("com.android.camera", "camera");
            scopeMap.put("com.android.calendar", "calendar");
            scopeMap.put("com.android.externalstorage", "external");
            scopeMap.put("com.android.fileexplorer", "file");
            scopeMap.put("com.android.incallui", "incallui");
            scopeMap.put("com.android.mms", "mms");
            scopeMap.put("com.android.nfc", "nfc");
            scopeMap.put("com.android.phone", "phone");
            scopeMap.put("com.android.providers.downloads", "download");
            scopeMap.put("com.android.providers.downloads.ui", "ui");
            scopeMap.put("com.android.systemui", "systemui");
            scopeMap.put("com.android.settings", "settings");
            scopeMap.put("com.android.thememanager", "theme");
            scopeMap.put("com.android.updater", "updater");
            scopeMap.put("com.lbe.security.miui", "lbe");
            scopeMap.put("com.milink.service", "milink");
            scopeMap.put("com.miui.aod", "aod");
            scopeMap.put("com.miui.backup", "backup");
            scopeMap.put("com.miui.cloudservice", "cloud");
            scopeMap.put("com.miui.contentextension", "content");
            scopeMap.put("com.miui.creation", "creation");
            scopeMap.put("com.miui.gallery", "gallery");
            scopeMap.put("com.miui.guardprovider", "guard");
            scopeMap.put("com.miui.home", "home");
            scopeMap.put("com.miui.huanji", "huan");
            scopeMap.put("com.miui.mediaeditor", "media");
            scopeMap.put("com.miui.mishare.connectivity", "share");
            scopeMap.put("com.miui.misound", "sound");
            scopeMap.put("com.miui.miwallpaper", "wallpaper");
            scopeMap.put("com.miui.notes", "notes");
            scopeMap.put("com.miui.packageinstaller", "install");
            scopeMap.put("com.miui.personalassistant", "personal");
            scopeMap.put("com.miui.powerkeeper", "power");
            scopeMap.put("com.miui.screenrecorder", "recorder");
            scopeMap.put("com.miui.screenshot", "shot");
            scopeMap.put("com.miui.securityadd", "add");
            scopeMap.put("com.miui.securitycenter", "center");
            scopeMap.put("com.miui.tsmclient", "tsmclient");
            scopeMap.put("com.miui.voiceassist", "voice");
            scopeMap.put("com.miui.weather2", "weather");
            scopeMap.put("com.xiaomi.aiasst.vision", "vision");
            scopeMap.put("com.xiaomi.barrage", "barrage");
            scopeMap.put("com.xiaomi.joyose", "joyose");
            scopeMap.put("com.xiaomi.market", "market");
            // scopeMap.put("com.xiaomi.mirror", "mirror");
            scopeMap.put("com.xiaomi.misettings", "misettings");
            scopeMap.put("com.xiaomi.mtb", "mtb");
            scopeMap.put("com.xiaomi.scanner", "scanner");
            scopeMap.put("com.xiaomi.trustservice", "trust");
            scopeMap.put("com.hchen.demo", "demo");*/
            return scopeMap;
        }
        return scopeMap;
    }

    /**
     * 交换 Map 中 Key 和 Value 位置。
     *
     * @return 交换后的 Map
     * @noinspection SameReturnValue
     */
    public static HashMap<String, String> swappedData() {
        if (scopeMap.isEmpty()) scopeData();
        if (!swappedMap.isEmpty()) return swappedMap;
        for (Map.Entry<String, String> entry : scopeMap.entrySet()) {
            swappedMap.put(entry.getValue(), entry.getKey());
        }
        return swappedMap;
    }

    public static boolean toPkgList(String pkg) {
        ArrayList<String> report = getReportCrashProp();
        for (String s : report) {
            String mPkg = swappedData().get(s);
            if (mPkg != null) {
                return mPkg.equals(pkg);
            }
        }
        return false;
    }

    public static ArrayList<String> toPkgList() {
        ArrayList<String> appCrash = new ArrayList<>();
        ArrayList<String> report = getReportCrashProp();
        for (String s : report) {
            String mPkg = swappedData().get(s);
            if (mPkg != null) {
                appCrash.add(mPkg);
            }
        }
        return appCrash;
    }

    public static ArrayList<String> getReportCrashProp() {
        String data = PropUtils.getProp("persist.hyperceiler.crash.report", "");
        if (data.isEmpty()) {
            return new ArrayList<>();
        }
        String[] sp = data.split(",");
        return new ArrayList<>(Arrays.asList(sp));
    }
}

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
            logE(TAG, "Failed to convert JSON!" + e);
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
            logE(TAG, "Failed to convert JSON!" + e);
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
            logE(TAG, "Failed to get package name!" + e);
        }
        return "null";
    }

    public static long getTime(JSONObject jsonObject) {
        try {
            return jsonObject.getLong("t");
        } catch (JSONException e) {
            logE(TAG, "Failed to get timestamp!" + e);
        }
        return -1L;
    }

    public static int getCount(JSONObject jsonObject) {
        try {
            return jsonObject.getInt("c");
        } catch (JSONException e) {
            logE(TAG, "Failed to get the number of times!" + e);
        }
        return -1;
    }

    public static JSONObject putParam(JSONObject jsonObject, long time, int count) {
        try {
            jsonObject.put("c", count);
            jsonObject.put("t", time);
            return jsonObject;
        } catch (JSONException e) {
            logE(TAG, "Failed to update data!" + e);
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
            logE(TAG, "Failed to convert Array!" + e);
        }
        return new ArrayList<>();
    }
}
