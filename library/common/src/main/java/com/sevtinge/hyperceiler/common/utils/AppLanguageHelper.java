package com.sevtinge.hyperceiler.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.log.AndroidLog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AppLanguageHelper {
    public static final String[] APP_LANGUAGES = {
        "en", "zh_CN", "zh_TW", "zh_HK", "ja_JP", "pl_PL", "ru_RU", "ar_SA", "es_ES",
        "pt_BR", "id_ID", "tr_TR", "vi_VN", "it_IT", "de_DE", "uk_UA", "zh_ME"
    };

    private static final Map<String, Locale> localeCache = new HashMap<>();

    private AppLanguageHelper() {
    }

    public static void init(Context context) {
        int languageSetting = AppSettingsStore.getAppLanguageIndex(context, -1);
        if (languageSetting != -1) {
            applyLanguage(context, languageSetting);
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

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static Locale getCachedLocale(String language, String country) {
        String key = country.isEmpty() ? language : language + "_" + country;
        return localeCache.computeIfAbsent(key, k -> new Locale(language, country));
    }

    public static void init(Activity activity) {
        int languageSetting = AppSettingsStore.getAppLanguageIndex(activity, -1);
        if (languageSetting != -1) {
            setIndexLanguage(activity, languageSetting, false);
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
        Locale locale = resources.getConfiguration().getLocales().isEmpty()
            ? null
            : resources.getConfiguration().getLocales().get(0);
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
        return 0;
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
