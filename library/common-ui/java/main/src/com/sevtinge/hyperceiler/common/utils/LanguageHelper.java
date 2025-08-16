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
package com.sevtinge.hyperceiler.common.utils;

import static com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mSharedPreferences;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LanguageHelper {
    public static final String[] appLanguages = {
        "en", "zh_CN", "zh_TW", "zh_HK", "ja_JP", "pl_PL", "ru_RU", "ar_SA", "es_ES", "pt_BR", "id_ID", "tr_TR", "vi_VN", "it_IT", "zh_ME"
    };

    private static final Map<String, Locale> localeCache = new HashMap<>();
    private static Locale currentLocale = Locale.getDefault();
    private static LocaleActivityLifecycleCallbacks lifecycleCallbacks;


    public static void init(Application app) {
        if (app == null) return;

        String languageSetting = mSharedPreferences.getString("prefs_key_settings_app_language", "-1");
        currentLocale = getCachedLocaleFromPrefs(languageSetting);

        // 写一次日志，便于排查（打印类加载器信息）
        AndroidLogUtils.logI("Language", "[init] LanguageHelper classloader=" + LanguageHelper.class.getClassLoader());
        AndroidLogUtils.logI("Language", "[init] prefs_key_settings_app_language = " + languageSetting);
        AndroidLogUtils.logI("Language", "[init] resolved initial locale = " + (currentLocale != null ? currentLocale.toLanguageTag() : "null"));

        // 应用到 application resources（尝试更新 app 资源）
        setLanguage(app.getApplicationContext(), currentLocale);

        // 注册 lifecycle callbacks（用于后续 Activity）
        registerLifecycleCallbacks(app, currentLocale);
    }

    public static void registerLifecycleCallbacks(Application app, Locale initialLocale) {
        if (app == null) return;
        if (lifecycleCallbacks == null) {
            lifecycleCallbacks = new LocaleActivityLifecycleCallbacks(initialLocale);
            try {
                app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
            } catch (Throwable t) {
                AndroidLogUtils.logE("Language", "Failed to register lifecycle callbacks", t);
            }
        } else {
            lifecycleCallbacks.setLocale(initialLocale);
        }
    }

    public static Context wrapContext(Context base) {
        if (base == null) return null;
        try {
            String languageSetting = mSharedPreferences != null ? mSharedPreferences.getString("prefs_key_settings_app_language", "-1") : "-1";
            Locale localeFromPrefs = getCachedLocaleFromPrefs(languageSetting);

            // 如果 prefs 有效，用 prefs 结果；否则回退到 currentLocale
            Locale useLocale = localeFromPrefs != null ? localeFromPrefs : currentLocale;

            Resources res = base.getResources();
            Configuration config = new Configuration(res.getConfiguration());
            if (useLocale != null) {
                config.setLocale(useLocale);

            }
            Context localized = base.createConfigurationContext(config);

            AndroidLogUtils.logI("Language", "wrapContext -> locale=" + (useLocale != null ? useLocale.toLanguageTag() : "null")
                + " (prefs=" + languageSetting + ")"
                + " classloader=" + LanguageHelper.class.getClassLoader());
            return localized;
        } catch (Throwable t) {
            AndroidLogUtils.logE("Language", "wrapContext failed", t);
            return base;
        }
    }

    public static void setLanguage(Context context, String language) {
        setLanguage(context, getCachedLocale(language, ""));
    }

    public static void setLanguage(Context context, String language, String country) {
        setLanguage(context, getCachedLocale(language, country));
    }

    public static void setLanguage(Context context, Locale locale) {
        if (context == null || locale == null) return;
        try {
            Locale old = Locale.getDefault();
            AndroidLogUtils.logI("Language", "[setLanguage] caller locale default = " + old.toLanguageTag());
            AndroidLogUtils.logI("Language", "[setLanguage] requested locale = " + locale.toLanguageTag());
            // 简单打印调用栈前几行帮助定位是谁触发的
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("[setLanguage] stack: ");
            for (int i = Math.min(6, st.length - 1); i >= 2; i--) {
                sb.append(st[i].getClassName()).append("#").append(st[i].getMethodName()).append(" -> ");
            }
            AndroidLogUtils.logI("Language", sb.toString());

            Resources resources = context.getResources();
            Configuration configuration = new Configuration(resources.getConfiguration());
            Locale.setDefault(locale);
            configuration.setLocale(locale);

            // 更新 Application 层资源（尽量更新 appContext 的 resources）
            Context appCtx = context.getApplicationContext();
            try {
                appCtx.getResources().updateConfiguration(configuration, appCtx.getResources().getDisplayMetrics());
            } catch (Throwable t) {
                AndroidLogUtils.logE("Language", "updateConfiguration failed on app context", t);
            }

            // 更新 lifecycle callbacks 持有的目标 locale（使新创建的 Activity 使用它）
            if (lifecycleCallbacks != null) {
                lifecycleCallbacks.setLocale(locale);
            }

            AndroidLogUtils.logI("Language", "Set language to " + locale.toLanguageTag());
        } catch (Throwable t) {
            AndroidLogUtils.logE("Language", "Failed to set language", t);
        }
    }

    public static void setIndexLanguage(Activity activity, int index, boolean recreate) {
        if (activity == null) return;
        if (index < 0 || index >= appLanguages.length) index = 0;
        String[] parts = appLanguages[index].split("_");
        Locale locale = getCachedLocale(parts[0], parts.length > 1 ? parts[1] : "");
        setLanguage(activity.getBaseContext(), locale);
        if (recreate) activity.recreate();
    }

    public static String getLanguage(Context context) {
        Resources resources = context.getResources();
        Locale locale = resources.getConfiguration().getLocales().isEmpty() ? null : resources.getConfiguration().getLocales().get(0);
        if (locale == null) {
            return "";
        }
        String country = locale.getCountry();
        return country.isEmpty() ? locale.getLanguage() : locale.getLanguage() + "_" + country;
    }

    private static Locale getCachedLocaleFromPrefs(String languageSetting) {
        if (languageSetting == null || "-1".equals(languageSetting)) return Locale.getDefault();
        int idx;
        try {
            idx = Integer.parseInt(languageSetting);
        } catch (Throwable t) {
            idx = 0;
        }
        if (idx < 0 || idx >= appLanguages.length) idx = 0;
        String[] parts = appLanguages[idx].split("_");
        return getCachedLocale(parts[0], parts.length > 1 ? parts[1] : "");
    }

    public static Locale getCachedLocale(String language, String country) {
        String key = language + "_" + country;
        if (localeCache.containsKey(key)) return localeCache.get(key);
        Locale locale;
        if (country == null || country.isEmpty()) {
            locale = new Locale(language);
        } else {
            locale = new Locale(language, country);
        }
        localeCache.put(key, locale);
        return locale;
    }

    public static boolean isUpperCase(String string) {
        return string.equals(string.toUpperCase());
    }

    public static String newLanguage(String language, String country) {
        return country == null || country.isEmpty() ? language : language + "_" + country;
    }

    public static int resultIndex(String[] languages, String value) {
        for (int i = 0; i < languages.length; i++) {
            if (languages[i] != null && languages[i].equals(value)) {
                AndroidLogUtils.logI("Language", "Match found: " + languages[i] + " at index " + i);
                return i;
            }
        }
        AndroidLogUtils.logE("Language", "No match found for: " + value);
        return 0; // 遇到错误就切回英语，防止崩溃
    }
}
