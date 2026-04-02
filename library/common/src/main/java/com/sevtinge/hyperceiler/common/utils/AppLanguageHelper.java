package com.sevtinge.hyperceiler.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AppLanguageHelper {
    public static final String[] APP_LANGUAGES = {
        "en", "zh_CN", "zh_TW", "zh_HK", "ja_JP", "pl_PL", "ru_RU", "ar_SA", "es_ES",
        "pt_BR", "id_ID", "tr_TR", "vi_VN", "it_IT", "de_DE", "uk_UA", "zh_ME"
    };
    private static final int[] APP_LANGUAGE_LABELS = {
        R.string.settings_app_en,
        R.string.settings_app_zh_cn,
        R.string.settings_app_zh_tw,
        R.string.settings_app_zh_hk,
        R.string.settings_app_ja_jp,
        R.string.settings_app_pl_pl,
        R.string.settings_app_ru_ru,
        R.string.settings_app_ar_sa,
        R.string.settings_app_es_es,
        R.string.settings_app_pt_br,
        R.string.settings_app_in_id,
        R.string.settings_app_tr_tr,
        R.string.settings_app_vi_vn,
        R.string.settings_app_it_it,
        R.string.settings_app_de_de,
        R.string.settings_app_uk_ua,
        R.string.settings_app_zh_me
    };

    private static final Map<String, Locale> localeCache = new HashMap<>();

    private AppLanguageHelper() {
    }

    public static void init(Context context) {
        int legacyIndex = AppSettingsStore.getAppLanguageIndex(context, -1);
        if (legacyIndex != -1) {
            updateContextResources(context, localeFromIndex(legacyIndex));
        }
    }

    public static void applyLanguage(Context context, int index) {
        int normalizedIndex = normalizeLanguageIndex(index);
        Locale locale = localeFromIndex(normalizedIndex);
        // 显式语言选择只在这里落盘，避免切换语言时出现重复写入。
        AppSettingsStore.setAppLanguageIndex(context, normalizedIndex);
        updateContextResources(context, locale);
    }

    public static Locale getCachedLocale(String language, String country) {
        String key = country.isEmpty() ? language : language + "_" + country;
        return localeCache.computeIfAbsent(key, k -> new Locale(language, country));
    }

    public static void init(Activity activity) {
        init((Context) activity);
    }

    public static void setLanguage(Context context, String language) {
        setLanguage(context, getCachedLocale(language, ""));
    }

    public static void setLanguage(Context context, String language, String country) {
        setLanguage(context, getCachedLocale(language, country));
    }

    public static void setLanguage(Context context, Locale locale) {
        syncStoredLanguageIndex(context, locale);
        updateContextResources(context, locale);
    }

    public static void setIndexLanguage(Activity activity, int index, boolean recreate) {
        applyLanguage(activity, index);
        // OOBE 仍允许整页重建，设置页则传 false 走无黑闪的软刷新。
        if (recreate && activity != null && !activity.isFinishing()) {
            activity.recreate();
        }
    }

    public static void clearLanguage(Context context) {
        AppSettingsStore.clearAppLanguageIndex(context);
        updateContextResources(context, getSystemLocale());
    }

    public static String getLanguage(Context context) {
        Locale locale = getCurrentLocale(context);
        String country = locale.getCountry();
        return country.isEmpty() ? locale.getLanguage() : locale.getLanguage() + "_" + country;
    }

    public static Locale getCurrentLocale(Context context) {
        int storedIndex = AppSettingsStore.getAppLanguageIndex(context, -1);
        if (storedIndex != -1) {
            return localeFromIndex(storedIndex);
        }

        LocaleList configurationLocales = context.getResources().getConfiguration().getLocales();
        if (!configurationLocales.isEmpty()) {
            return configurationLocales.get(0);
        }
        return getSystemLocale();
    }

    public static int getCurrentLanguageIndex(Context context) {
        return findBestLanguageIndex(getCurrentLocale(context));
    }

    public static CharSequence[] getLanguageEntries(Context context) {
        CharSequence[] entries = new CharSequence[APP_LANGUAGE_LABELS.length];
        for (int i = 0; i < APP_LANGUAGE_LABELS.length; i++) {
            entries[i] = context.getText(APP_LANGUAGE_LABELS[i]);
        }
        return entries;
    }

    public static CharSequence[] getLanguageEntryValues() {
        CharSequence[] values = new CharSequence[APP_LANGUAGES.length];
        for (int i = 0; i < APP_LANGUAGES.length; i++) {
            values[i] = Integer.toString(i);
        }
        return values;
    }

    public static boolean hasExplicitLanguage(Context context) {
        return AppSettingsStore.getAppLanguageIndex(context, -1) != -1;
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

    public static Locale localeFromIndex(int index) {
        return localeFromAppLanguage(APP_LANGUAGES[normalizeLanguageIndex(index)]);
    }

    private static void syncStoredLanguageIndex(Context context, Locale locale) {
        int currentIndex = findBestLanguageIndex(locale);
        AppSettingsStore.setAppLanguageIndex(context, currentIndex);
    }

    private static int findBestLanguageIndex(Locale locale) {
        String current = newLanguage(locale.getLanguage(), locale.getCountry());
        int exactIndex = indexOfLanguageValue(current);
        if (exactIndex != 0 || APP_LANGUAGES[0].equals(current)) {
            return exactIndex;
        }

        String language = locale.getLanguage();
        for (int i = 0; i < APP_LANGUAGES.length; i++) {
            Locale candidate = localeFromAppLanguage(APP_LANGUAGES[i]);
            if (TextUtils.equals(candidate.getLanguage(), language)) {
                return i;
            }
        }
        return 0;
    }

    private static int indexOfLanguageValue(String value) {
        for (int i = 0; i < APP_LANGUAGES.length; i++) {
            if (TextUtils.equals(APP_LANGUAGES[i], value)) {
                return i;
            }
        }
        return 0;
    }

    private static int normalizeLanguageIndex(int index) {
        return index >= 0 && index < APP_LANGUAGES.length ? index : 0;
    }

    public static Context wrapContext(Context base) {
        int storedIndex = AppSettingsStore.getAppLanguageIndex(base, -1);
        if (storedIndex == -1) {
            return base;
        }
        // attachBaseContext 只在用户已明确选过语言时包裹，首次 OOBE 仍跟系统。
        Locale locale = localeFromIndex(storedIndex);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        LocaleList localeList = new LocaleList(locale);
        configuration.setLocales(localeList);
        configuration.setLocale(locale);
        return base.createConfigurationContext(configuration);
    }

    private static void updateContextResources(Context context, Locale locale) {
        LocaleList localeList = new LocaleList(locale);
        LocaleList.setDefault(localeList);
        Locale.setDefault(locale);
        updateResources(context.getResources(), locale, localeList);

        Context appContext = context.getApplicationContext();
        if (appContext != null && appContext != context) {
            updateResources(appContext.getResources(), locale, localeList);
        }
    }

    private static void updateResources(Resources resources, Locale locale, LocaleList localeList) {
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocales(localeList);
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    private static Locale getSystemLocale() {
        LocaleList systemLocales = Resources.getSystem().getConfiguration().getLocales();
        if (!systemLocales.isEmpty()) {
            return systemLocales.get(0);
        }
        return Locale.getDefault();
    }
}
