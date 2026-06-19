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

import static android.os.Process.killProcess;
import static android.os.Process.myPid;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSmallVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.utils.LanguageHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fan.appcompat.app.AlertDialog;

public class NoticeProcessor {
    private static final String NOTICE_URL = "https://api-hyperceiler.sevtinge.com/app.json";

    /**
     * Main entry point, used to check whether the display conditions are met and to package the display results
     */
    public static NoticeResult process(android.content.Context context) {
        try {
            String json = request(NOTICE_URL);
            AndroidLog.i("NoticeProcessor", "Got notice.");
            if (json == null || json.isEmpty()) return new NoticeResult(
                null,
                null,
                -100,
                -100,
                -100,
                -100
            );

            Notice notice = parseNotice(new JSONObject(json));

            // Display conditions
            if (!checkNoticeValid(notice, context)) {
                return new NoticeResult(
                    null,
                    null,
                    -100,
                    -100,
                    notice.protocolVersion,
                    notice.privacyVersion
                );
            }

            AndroidLog.i("NoticeProcessor", "Notice is valid. Show notice.");

            // Package result
            return new NoticeResult(
                notice.title,
                notice.content,
                notice.confirmDelaySeconds,
                notice.id,
                notice.protocolVersion,
                notice.privacyVersion
            );

        } catch (Throwable t) {
            AndroidLog.e("NoticeProcessor", "Failed when request notice: " + Log.getStackTraceString(t));
            return null;
        }
    }


    /**
     * Network request
     */
    private static String request(String urlStr) throws Exception {
        HttpURLConnection conn =
            (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", buildUserAgent());
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

    private static String buildUserAgent() {
        return "HyperCeiler/"
            + BuildConfig.VERSION_NAME
            + " ("
            + "AndroidSDK "
            + getAndroidVersion()
            + "; HyperOS "
            + getHyperOSVersion() + ", " + getSmallVersion()
            + "; "
            + BuildConfig.BUILD_TYPE
            + "; "
            + BuildConfig.GIT_HASH
            + ")";
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

        n.protocolVersion = obj.optInt("protocolVersion", -1);
        n.privacyVersion = obj.optInt("privacyVersion", -1);

        AndroidLog.i("NoticeProcessor", "NoticeId = " + n.id);

        return n;
    }


    /**
     * Check new notice valid
     */
    private static boolean checkNoticeValid(Notice n, Context context) {
        long now = System.currentTimeMillis() / 1000;
        if (!n.alwaysShow && n.id == PrefsBridge.getInt("prefs_key_notice_id", 0)) return false;

        // Time window
        if (n.startTime > 0 && now < n.startTime && n.startTime != -1L) return false;
        if (n.endTime > 0 && now > n.endTime && n.endTime != -1L) return false;

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
        String lang = LanguageHelper.getCurrentLocale(context).toLanguageTag();
        if (!matchStringListAllowAll(n.lang, lang)) {
            return false;
        }

        // Is need sign check
        return !n.signCheckPassNeed || SignUtils.isSignCheckPass(context);
    }

    public static boolean isNeedShowTosDialog(NoticeProcessor.NoticeResult result){
        if (result == null) return false;
        if (result.protocolVersion == -1 || result.privacyVersion == -1) return false;
        if (PrefsBridge.getInt("prefs_key_protocol_version", -1) >= result.protocolVersion && PrefsBridge.getInt("prefs_key_privacy_version", -1) >= result.privacyVersion) return false;
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

        public int protocolVersion;
        public int privacyVersion;
    }

    public record NoticeResult(String title, String content, int confirmDelaySeconds, int id, int protocolVersion, int privacyVersion) {
    }

    public static void showTosDialog(Context context, NoticeProcessor.NoticeResult result) {
        if (result == null) return;

        int textColor = ContextCompat.getColor(context, R.color.textview_black);
        int linkColor = ContextCompat.getColor(context, R.color.textview_blue);

        CharSequence raw = context.getText(R.string.tos_update_desc);
        SpannableString ss = new SpannableString(raw);

        ss.setSpan(new ForegroundColorSpan(textColor), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Annotation[] anns = ss.getSpans(0, ss.length(), Annotation.class);
        for (Annotation an : anns) {
            int start = ss.getSpanStart(an);
            int end = ss.getSpanEnd(an);
            String key = an.getValue(); // "protocol" or "privacy"
            ss.removeSpan(an);

            ClickableSpan span;
            if ("protocol".equals(key)) {
                span = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://hyperceiler.sevtinge.com/Protocol")));
                    }
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setColor(linkColor);
                        ds.setUnderlineText(true);
                    }
                };
            } else if ("privacy".equals(key)) {
                span = new ClickableSpan() {
                    @Override public void onClick(@NonNull View widget) {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://hyperceiler.sevtinge.com/Privacy")));
                    }
                    @Override public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setColor(linkColor);
                        ds.setUnderlineText(true);
                    }
                };
            } else {
                continue;
            }
            ss.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        TextView msgView = new TextView(context);
        msgView.setText(ss);
        msgView.setMovementMethod(LinkMovementMethod.getInstance());
        msgView.setPadding(dp2px(context, 24), dp2px(context, 12), dp2px(context, 24), dp2px(context, 24));
        msgView.setTextSize(16);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(R.string.tos_update_title)
            .setView(msgView)
            .setPositiveButton(com.sevtinge.hyperceiler.core.R.string.new_cta_app_all_purpose_agree, (d, which) -> {
                PrefsBridge.putByApp("prefs_key_protocol_version", result.protocolVersion);
                PrefsBridge.putByApp("prefs_key_privacy_version", result.privacyVersion);
            })
            .setNegativeButton(com.sevtinge.hyperceiler.core.R.string.new_cta_app_all_purpose_reject, (d, which) -> {
                if (context instanceof Activity) {
                    ((Activity) context).finishAffinity();
                    killProcess(myPid());
                }
            })
            .create();

        dialog.show();
    }

    public static void showNoticeDialog(Context context, NoticeProcessor.NoticeResult result) {
        if (result == null) return;

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(result.title)
            .setMessage(result.content)
            .setPositiveButton(android.R.string.ok, (d, which) -> {
                PrefsBridge.putByApp("prefs_key_notice_id", result.id);
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
