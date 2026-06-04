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
package com.sevtinge.hyperceiler.hooker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import fan.preference.DropDownPreference;

/**
 * AI 复制直达浏览器替换的偏好页。运行时枚举可用浏览器并把选择写入 {@code prefs_key_aicr_browser}，
 * value 直接是包名；空字符串表示「跟随系统默认浏览器」。Hook 端从同一个 key 里读结果。
 */
public class AicrFragment extends DashboardFragment {

    private static final String KEY_BROWSER = "prefs_key_aicr_browser";
    private static final String VALUE_FOLLOW_SYSTEM = "";

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.aicr;
    }

    @Override
    public void initPrefs() {
        DropDownPreference browser = findPreference(KEY_BROWSER);
        if (browser == null) return;

        populateBrowserChoices(browser);
        if (TextUtils.isEmpty(browser.getValue())) {
            browser.setValue(VALUE_FOLLOW_SYSTEM);
        }
    }

    private void populateBrowserChoices(DropDownPreference browser) {
        Context context = getContext();
        if (context == null) return;

        List<ResolveInfo> browserInfos = queryBrowsers(context);

        List<CharSequence> entries = new ArrayList<>(browserInfos.size() + 1);
        List<CharSequence> values = new ArrayList<>(browserInfos.size() + 1);
        entries.add(context.getString(R.string.aicr_default_browser));
        values.add(VALUE_FOLLOW_SYSTEM);
        PackageManager pm = context.getPackageManager();
        for (ResolveInfo info : browserInfos) {
            entries.add(info.loadLabel(pm));
            values.add(info.activityInfo.packageName);
        }

        browser.setEntries(entries.toArray(new CharSequence[0]));
        browser.setEntryValues(values.toArray(new CharSequence[0]));
    }

    /**
     * 取 http ∩ https 的包名集合，按 label 排序；同包名只保留首次出现的 ResolveInfo。
     */
    private List<ResolveInfo> queryBrowsers(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> httpInfos = queryViewActivities(pm, "http:");
        List<ResolveInfo> httpsInfos = queryViewActivities(pm, "https:");

        LinkedHashMap<String, Boolean> httpsPkgs = new LinkedHashMap<>();
        for (ResolveInfo info : httpsInfos) {
            if (info.activityInfo != null && info.activityInfo.packageName != null) {
                httpsPkgs.put(info.activityInfo.packageName, Boolean.TRUE);
            }
        }

        String selfPkg = context.getPackageName();
        LinkedHashMap<String, ResolveInfo> uniq = new LinkedHashMap<>();
        for (ResolveInfo info : httpInfos) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (TextUtils.isEmpty(pkg) || "android".equals(pkg) || pkg.equals(selfPkg)) continue;
            if (!httpsPkgs.containsKey(pkg)) continue;
            uniq.putIfAbsent(pkg, info);
        }

        List<ResolveInfo> result = new ArrayList<>(uniq.values());
        Collator collator = Collator.getInstance(Locale.getDefault());
        result.sort((a, b) -> collator.compare(
            a.loadLabel(pm).toString(), b.loadLabel(pm).toString()));
        return result;
    }

    private List<ResolveInfo> queryViewActivities(PackageManager pm, String scheme) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addCategory(Intent.CATEGORY_DEFAULT);
        try {
            List<ResolveInfo> r = pm.queryIntentActivities(intent,
                PackageManager.MATCH_ALL | PackageManager.MATCH_DEFAULT_ONLY);
            return r == null ? new ArrayList<>() : r;
        } catch (Throwable ignored) {
            return new ArrayList<>();
        }
    }
}
