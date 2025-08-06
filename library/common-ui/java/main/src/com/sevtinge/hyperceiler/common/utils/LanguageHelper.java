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
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.util.Locale;

public class LanguageHelper {
    public static final String[] appLanguages = {
            "en", "zh_CN", "zh_TW", "zh_HK", "ja_JP", "pl_PL", "ru_RU", "ar_SA", "es_ES", "pt_BR", "id_ID", "tr_TR", "vi_VN", "it_IT", "zh_ME"
    };

    private static final java.util.Map<String, Locale> localeCache = new java.util.HashMap<>();

    public static void init(Activity activity) {
        String languageSetting = mSharedPreferences.getString("prefs_key_settings_app_language", "-1");
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

    public static String newLanguage(String language, String country) {
        return country.isEmpty() ? language : language + "_" + country;
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

    public static boolean isUpperCase(String string) {
        return string.equals(string.toUpperCase());
    }

    private static Locale getCachedLocale(String language, String country) {
        String key = country.isEmpty() ? language : language + "_" + country;
        return localeCache.computeIfAbsent(key, k -> country.isEmpty() ? new Locale(language) : new Locale(language, country));
    }
}
