package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageHelper {
    public static final String[] appLanguages = {
            "CN", "TW", "HK", "en", "JP"
    };

    public static void setLanguage(Context context, String language) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static void setLanguage(Context context, String language, String country) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = new Locale(language, country);
        Locale.setDefault(locale);
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static void setLanguage(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale.setDefault(locale);
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static String getLanguage(Context context) {
        Resources resources = context.getResources();
        String country = resources.getConfiguration().getLocales().get(0).getCountry();
        if (country.isEmpty()) {
            return resources.getConfiguration().getLocales().get(0).getLanguage();
        }
        return country;
    }

    public static String newLanguage(String language, String country) {
        if (country.isEmpty()) {
            return new Locale(language, country).getLanguage();
        }
        return new Locale(language, country).getCountry();
    }

    public static int resultIndex(String[] languages, String value) {
        int index = -1;
        for (String l : languages) {
            index = index + 1;
            if (l.equals(value)) {
                return index;
            }
        }
        return index;
    }

    public static boolean isUpperCase(String string) {
        String result = string.toUpperCase();
        return result.equals(string);
    }
}
