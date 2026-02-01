/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.appLanguages;
import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.localeFromAppLanguage;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSmallVersion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fan.appcompat.app.AlertDialog;

public class NoticeProcessor {
    private static final String NOTICE_URL = "https://api-hyperceiler.sevtinge.com/app.json";

    /**
     * Main entry point, used to check whether the display conditions are met and to package the display results
     */
    public static NoticeResult process(Context context) {
        AndroidLog.e("NoticeProcessor running");
        try {
            String json = request(NOTICE_URL);
            AndroidLog.e("NoticeProcessor "+json);
            if (json == null || json.isEmpty()) return null;

            Notice notice = parseNotice(new JSONObject(json));

            // Display conditions
            if (!checkNoticeValid(notice, context)) {
                return null;
            }

            // Package result
            return new NoticeResult(
                notice.title,
                notice.content,
                notice.confirmDelaySeconds,
                notice.id
            );

        } catch (Throwable t) {
            AndroidLog.e("NoticeProcessor "+Log.getStackTraceString(t));
            return null;
        }
    }


    /**
     * Network request
     */
    private static String request(String urlStr) throws Exception {
        HttpURLConnection conn =
            (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * JSON2Notice
     */
    private static Notice parseNotice(JSONObject obj) {
        Notice n = new Notice();

        n.id = obj.optInt("id");
        n.alwaysShow = obj.optBoolean("alwaysShow");
        n.title = obj.optString("title");
        n.content = obj.optString("content");
        n.confirmDelaySeconds = obj.optInt("confirmDelaySeconds", 0);
        n.startTime = obj.optLong("startTime");
        n.endTime = obj.optLong("endTime");
        n.versionMin = obj.optInt("versionMin", -1);
        n.versionMax = obj.optInt("versionMax", -1);
        n.signCheckPassNeed = obj.optBoolean("signCheckPassNeed", false);

        n.buildType = toStringList(obj.optJSONArray("buildType"));
        n.androidVersion = toIntList(obj.optJSONArray("androidVersion"));
        n.miuiBigVersion = toFloatList(obj.optJSONArray("miuiBigVersion"));
        n.miuiSmallVersion = toFloatList(obj.optJSONArray("miuiSmallVersion"));
        n.lang = toStringList(obj.optJSONArray("lang"));

        return n;
    }


    /**
     * Check new notice valid
     */
    private static boolean checkNoticeValid(Notice n, Context context) {
        long now = System.currentTimeMillis() / 1000;
        if (!n.alwaysShow && n.id == PrefsUtils.getSharedIntPrefs(context, "prefs_key_notice_id", 0)) return false;

        // Time window
        if (n.startTime > 0 && now < n.startTime) return false;
        if (n.endTime > 0 && now > n.endTime) return false;

        // App version
        int versionCode = BuildConfig.VERSION_CODE;
        if (n.versionMin >= 0 && versionCode < n.versionMin) return false;
        if (n.versionMax >= 0 && versionCode > n.versionMax) return false;

        // Build Type
        if (!matchStringListAllowAll(n.buildType, BuildConfig.BUILD_TYPE)) {
            return false;
        }

        // Android Version
        if (!matchIntListAllowAll(n.androidVersion, getAndroidVersion())) {
            return false;
        }

        // HyperOS big version
        if (!matchFloatListAllowAll(n.miuiBigVersion, getHyperOSVersion())) {
            return false;
        }

        // HyperOS small version
        if (!matchFloatListAllowAll(n.miuiSmallVersion, getSmallVersion())) {
            return false;
        }

        // HyperCeiler language
        int selectedLang = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "prefs_key_settings_app_language", "0"));
        if (selectedLang < 0 || selectedLang >= appLanguages.length) selectedLang = 0;
        Locale locale = localeFromAppLanguage(appLanguages[selectedLang]);
        String lang = locale.toLanguageTag();
        if (!matchStringListAllowAll(n.lang, lang)) {
            return false;
        }

        // Is need sign check
        if (n.signCheckPassNeed && !SignUtils.isSignCheckPass(context)) {
            return false;
        }

        return true;
    }

    private static boolean matchStringList(List<String> list, String value) {
        if (list == null || list.isEmpty()) return true;
        for (String v : list) {
            if (value.equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchStringListAllowAll(List<String> list, String value) {
        if (list == null || list.isEmpty()) return true;
        for (String v : list) {
            if ("all".equalsIgnoreCase(v) || value.equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchIntList(List<Integer> list, int value) {
        if (list == null || list.isEmpty()) return true;
        for (Integer v : list) {
            if (v != null && v == value) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchIntListAllowAll(List<Integer> list, int value) {
        if (list == null || list.isEmpty()) return true;
        for (Integer v : list) {
            if (v != null && (v == value || v == -1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchFloatList(List<Float> list, float value) {
        if (list == null || list.isEmpty()) return true;
        for (Float v : list) {
            if (v != null && (Float.compare(v, value) == 0)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchFloatListAllowAll(List<Float> list, float value) {
        if (list == null || list.isEmpty()) return true;
        for (Float v : list) {
            if (v != null && (Float.compare(v, value) == 0 || Float.compare(v, -1f) == 0)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> toStringList(JSONArray arr) {
        if (arr == null || arr.length() == 0) return null;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.optString(i));
        }
        return list;
    }

    private static List<Integer> toIntList(JSONArray arr) {
        if (arr == null || arr.length() == 0) return null;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.optInt(i));
        }
        return list;
    }

    private static List<Float> toFloatList(JSONArray arr) {
        if (arr == null || arr.length() == 0) return null;
        List<Float> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add((float) arr.optDouble(i));
        }
        return list;
    }


    /**
     * Notice Data Structure
     */
    public static class Notice {
        public int id;
        public boolean alwaysShow;
        public String title;
        public String content;

        /**
         * -1：never allow continue
         *  0：continue at time
         * >0：delay seconds to continue
         */
        public int confirmDelaySeconds;

        public long startTime;
        public long endTime;

        public int versionMin;
        public int versionMax;

        public List<String> buildType;
        public List<Integer> androidVersion;
        public List<Float> miuiBigVersion;
        public List<Float> miuiSmallVersion;
        public List<String> lang;

        public boolean signCheckPassNeed;
    }

    public static class NoticeResult {
        public final String title;
        public final String content;
        public final int confirmDelaySeconds;
        public final int id;

        public NoticeResult(String title, String content, int confirmDelaySeconds, int id) {
            this.title = title;
            this.content = content;
            this.confirmDelaySeconds = confirmDelaySeconds;
            this.id = id;
        }
    }

    public static void showNoticeDialog(Context context, NoticeProcessor.NoticeResult result) {
        if (result == null) return;

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(result.title)
            .setMessage(result.content)
            .setPositiveButton(android.R.string.ok, (d, which) -> {
                PrefsUtils.putInt("prefs_key_notice_id", result.id);
            })
            .create();

        dialog.show();

        if (result.confirmDelaySeconds != 0) {
            delayPositiveButton(dialog, result.confirmDelaySeconds, context);
        }
    }

    private static void delayPositiveButton(AlertDialog dialog, int seconds, Context context) {
        final var button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button == null) return;

        if (seconds < 0) {
            button.setEnabled(false);
            return;
        }

        button.setEnabled(false);

        new android.os.CountDownTimer(seconds * 1000L, 1000L) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                button.setText(context.getString(android.R.string.ok) + " (" + millisUntilFinished / 1000 + ")");
            }

            @Override
            public void onFinish() {
                button.setEnabled(true);
                button.setText(android.R.string.ok);
            }
        }.start();
    }

}
