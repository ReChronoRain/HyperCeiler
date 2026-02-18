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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.common.utils;

import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.appLanguages;
import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.localeFromAppLanguage;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.model.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.common.model.data.ModData;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 搜索索引管理器。
 * <p>
 * 使用方式：
 * - 应用启动时调用 {@link #buildIndex(Context, int, boolean)} 构建全量索引
 * - 搜索时调用 {@link #search(String, Locale)} 获取结果
 */
public final class SearchIndexManager {

    private static final Set<String> SKIP_TAGS = Set.of(
        "PreferenceScreen",
        "PreferenceCategory",
        "com.sevtinge.hyperceiler.common.prefs.LayoutPreference"
        // 过滤组件, 避免被索引到
    );
    private static final String TAG = "SearchIndexManager";
    public static final int MARK_COLOR = Color.rgb(255, 0, 0);
    public static final String NEW_MODS_QUERY = "\uD83C\uDD95";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String APP_NS = "http://schemas.android.com/apk/res-auto";
    public static final HashSet<String> NEW_MODS = new HashSet<>(Set.of("pref_key_launcher_nozoomanim"));

    private static final CopyOnWriteArrayList<ModData> INDEX = new CopyOnWriteArrayList<>();
    private static final Set<Integer> REGISTERED = Collections.synchronizedSet(new HashSet<>());

    private SearchIndexManager() {}

    public static List<ModData> getIndex() {
        return Collections.unmodifiableList(INDEX);
    }

    // 入口：主模块传入 prefs_main 的资源 ID
    public static void buildIndex(Context context, int mainPrefsXmlResId, boolean force) {
        if (force) {
            INDEX.clear();
            REGISTERED.clear();
        } else if (!INDEX.isEmpty()) {
            return;
        }
        ThreadPoolManager.getInstance().submit(() -> scanMainPrefs(context.getApplicationContext(), mainPrefsXmlResId));
    }

    public static List<ModData> search(String query, Locale locale) {
        if (TextUtils.isEmpty(query)) return Collections.emptyList();

        boolean isChinese = locale.getLanguage().equals(new Locale("zh").getLanguage());
        String lowerQuery = query.toLowerCase(locale);
        Set<String> seen = new HashSet<>();
        List<ModData> results = new ArrayList<>();

        if (query.equals(NEW_MODS_QUERY)) {
            for (ModData mod : INDEX) {
                if (NEW_MODS.contains(mod.key) && seen.add(mod.key)) results.add(mod);
            }
            return results;
        }

        if (isChinese) {
            for (ModData mod : INDEX) {
                String titleLower = mod.title.toLowerCase(locale);
                for (int i = 0; i < lowerQuery.length(); i++) {
                    if (titleLower.indexOf(lowerQuery.charAt(i)) >= 0) {
                        if (seen.add(mod.key)) results.add(mod);
                        break;
                    }
                }
            }
            results.sort((a, b) -> Integer.compare(
                charFrequency(b.title.toLowerCase(locale), lowerQuery),
                charFrequency(a.title.toLowerCase(locale), lowerQuery)));
        } else {
            for (ModData mod : INDEX) {
                if (mod.title.toLowerCase(locale).contains(lowerQuery) && seen.add(mod.key))
                    results.add(mod);
            }
            results.sort((a, b) -> {
                int cmp = a.getGroup().compareToIgnoreCase(b.getGroup());
                return cmp != 0 ? cmp : a.title.compareToIgnoreCase(b.title);
            });
        }
        return results;
    }

    private static int charFrequency(String title, String query) {
        int count = 0;
        for (int i = 0; i < query.length(); i++) {
            if (title.indexOf(query.charAt(i)) >= 0) count++;
        }
        return count;
    }

    // ---- 扫描逻辑 ----

    private static Resources getLocaleResources(Context appContext) {
        int selectedLang = Integer.parseInt(
            PrefsUtils.getSharedStringPrefs(appContext, "prefs_key_settings_app_language", "0"));
        if (selectedLang < 0 || selectedLang >= appLanguages.length) selectedLang = 0;
        Locale locale = localeFromAppLanguage(appLanguages[selectedLang]);
        Configuration config = new Configuration(appContext.getResources().getConfiguration());
        config.setLocale(locale);
        return appContext.createConfigurationContext(config).getResources();
    }

    private static void scanMainPrefs(Context appContext, int mainPrefsXmlResId) {
        Resources res = getLocaleResources(appContext);
        Map<String, String> groupToPackage = new LinkedHashMap<>();

        try (XmlResourceParser xml = res.getXml(mainPrefsXmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.getName().contains("PreferenceHeader")) {
                    String fragment = xml.getAttributeValue(ANDROID_NS, "fragment");
                    String titleAttr = xml.getAttributeValue(ANDROID_NS, "title");
                    String summaryAttr = xml.getAttributeValue(ANDROID_NS, "summary");
                    String title = resolveStringRef(res, titleAttr);
                    String packageName = resolveStringRef(res, summaryAttr);
                    if (packageName == null) packageName = summaryAttr;
                    int inflatedXml = resolveResId(xml.getAttributeValue(APP_NS, "inflatedXml"));

                    if (title != null && packageName != null) {
                        groupToPackage.put(title, packageName);
                    }

                    if (inflatedXml > 0) {
                        String frag = fragment != null ? fragment : DashboardFragment.class.getName();
                        scanEntryXml(res, frag, inflatedXml, title);
                    } else if (fragment != null) {
                        int xmlResId = getXmlResIdFromFragment(fragment);
                        if (xmlResId > 0) {
                            scanEntryXml(res, fragment, xmlResId, title);
                        }
                    }
                }
                eventType = xml.next();
            }
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to scan prefs_main", t);
        }

        ModSearchAdapter.initGroupPackageMap(groupToPackage);
    }

    private static void scanEntryXml(Resources res, String fragment, int xmlResId, String parentTitle) {
        if (!REGISTERED.add(xmlResId)) return;

        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            int order = 0;
            String location = null;
            int locationId = 0;
            List<ModData> batch = new ArrayList<>();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xml.getName();

                    if ("PreferenceScreen".equals(tag)) {
                        String myLoc = xml.getAttributeValue(APP_NS, "myLocation");
                        if (myLoc != null && location == null) {
                            location = resolveStringRef(res, myLoc);
                            locationId = resolveResId(myLoc);
                        }
                    } else if (!SKIP_TAGS.contains(tag)) {
                        String titleAttr = xml.getAttributeValue(ANDROID_NS, "title");
                        String keyAttr = xml.getAttributeValue(ANDROID_NS, "key");
                        String isVisibleAttr = xml.getAttributeValue(APP_NS, "isPreferenceVisible");
                        String childFragment = xml.getAttributeValue(ANDROID_NS, "fragment");

                        String modTitle = resolveStringRef(res, titleAttr);
                        boolean isHidden = "false".equals(isVisibleAttr);

                        // 递归子页面
                        if (childFragment != null) {
                            int childXml = getXmlResIdFromFragment(childFragment);
                            if (childXml > 0) {
                                String breadcrumb = buildBreadcrumb(parentTitle, location);
                                scanEntryXml(res, childFragment, childXml, breadcrumb);
                            }
                        }
                        // 索引当前项
                        if (!TextUtils.isEmpty(modTitle) && !TextUtils.isEmpty(keyAttr) && !isHidden) {
                            ModData mod = new ModData();
                            mod.title = modTitle;
                            mod.key = keyAttr;
                            mod.xml = xmlResId;
                            mod.order = order;
                            mod.fragment = fragment;
                            mod.breadcrumbs = buildBreadcrumb(parentTitle, location);
                            mod.catTitleResId = locationId;
                            batch.add(mod);
                        }
                        order++;
                    }
                }
                eventType = xml.next();
            }

            if (!batch.isEmpty()) INDEX.addAll(batch);
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to parse XML: " + xmlResId, t);
        }
    }

    private static int getXmlResIdFromFragment(String fragmentName) {
        try {
            Class<?> clazz = Class.forName(fragmentName);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method method = findMethod(clazz, "getPreferenceScreenResId");
            if (method != null) {
                method.setAccessible(true);
                Object result = method.invoke(instance);
                if (result instanceof Integer) return (int) result;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String name) {
        while (clazz != null) {
            try { return clazz.getDeclaredMethod(name); }
            catch (NoSuchMethodException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    private static String buildBreadcrumb(String parent, String location) {
        if (parent == null && location == null) return "";
        if (parent == null) return location;
        if (location == null) return parent;
        if (parent.equals(location)) return parent;
        return parent + "/" + location;
    }


    private static String resolveStringRef(Resources res, String attr) {
        if (attr == null) return null;
        if (attr.startsWith("@")) {
            try {
                int id = Integer.parseInt(attr.substring(1));
                if (id > 0) return res.getString(id);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static int resolveResId(String attr) {
        if (attr == null || !attr.startsWith("@")) return 0;
        try {
            int id = Integer.parseInt(attr.substring(1));
            return Math.max(id, 0);
        } catch (NumberFormatException e) { return 0; }
    }

    public static void clear() {
        INDEX.clear();
        REGISTERED.clear();
    }
}
