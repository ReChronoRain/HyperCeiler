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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LanguageHelper {
    public static final String[] APP_LANGUAGES = {
            "en", "zh_CN", "zh_TW", "zh_HK", "ja_JP", "pl_PL", "ru_RU", "ar_SA", "es_ES", "pt_BR", "id_ID", "tr_TR", "vi_VN", "it_IT", "de_DE", "uk_UA", "zh_ME"
    };

    private static final Map<String, Locale> localeCache = new HashMap<>();

    /**
     * 优化后的初始化：接受 Context 即可，可在 Application 中预热
     */
    public static void init(Context context) {
        String languageSetting = PrefsBridge.getString("prefs_key_settings_app_language", "-1");
        if (!"-1".equals(languageSetting)) {
            applyLanguage(context, Integer.parseInt(languageSetting));
        }
    }

    public static void applyLanguage(Context context, int index) {
        if (index < 0 || index >= APP_LANGUAGES.length) index = 0;
        String[] parts = APP_LANGUAGES[index].split("_");
        Locale locale = getCachedLocale(parts[0], parts.length > 1 ? parts[1] : "");
        updateResourceConfiguration(context, locale);
    }

    private static void updateResourceConfiguration(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale.setDefault(locale);

        configuration.setLocale(locale);
        LocaleList localeList = new LocaleList(locale);
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);

        // 兼容性更新资源配置
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static Locale getCachedLocale(String language, String country) {
        String key = country.isEmpty() ? language : language + "_" + country;
        return localeCache.computeIfAbsent(key, k -> new Locale(language, country));
    }




    public static void init(Activity activity) {
        String languageSetting = PrefsBridge.getString("prefs_key_settings_app_language", "-1");
        if (!"-1".equals(languageSetting)) {
            setIndexLanguage(activity, Integer.parseInt(languageSetting), false);
        }
    }

    public static void setLanguage(Context context, String language) {
        setLanguage(context, getCachedLocale(language, ""));
    }

    public static void setLanguage(Context context, String language, String country) {
        setLanguage(context, getCachedLocale(language, country));
    }

    public static void setLanguage(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale.setDefault(locale);
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static void setIndexLanguage(Activity activity, int index, boolean recreate) {
        if (index < 0 || index >= APP_LANGUAGES.length) index = 0;
        String[] parts = APP_LANGUAGES[index].split("_");
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

    public static String newLanguage(String language, String country) {
        return country.isEmpty() ? language : language + "_" + country;
    }

    public static int resultIndex(String[] languages, String value) {
        for (int i = 0; i < languages.length; i++) {
            if (languages[i] != null && languages[i].equals(value)) {
                AndroidLog.i("Language", "Match found: " + languages[i] + " at index " + i);
                return i;
            }
        }
        AndroidLog.e("Language", "No match found for: " + value);
        return 0; // 遇到错误就切回英语，防止崩溃
    }

    public static boolean isUpperCase(String string) {
        return string.equals(string.toUpperCase());
    }

    public static Locale localeFromAppLanguage(String lang) {
        if (TextUtils.isEmpty(lang)) {
            return Locale.ENGLISH;
        }

        String[] parts = lang.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return Locale.ENGLISH;
        }
    }

}
