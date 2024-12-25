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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageHelper {
    public static final String[] appLanguages = {
            "en", "zh_CN", "zh_TW", "zh_HK", "ja_JP", "pl_PL", "ru_RU", "ar_SA", "es_ES", "pt_BR", "in_ID", "tr_TR", "vi_VN", "it_IT", "zh_ME"
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

    public static void setIndexLanguage(Activity activity, int index, boolean recreate) {
        switch (index) {
            case 0 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "en");
                if (recreate) activity.recreate();
            }
            case 1 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "zh", "CN");
                if (recreate) activity.recreate();
            }
            case 2 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "zh", "TW");
                if (recreate) activity.recreate();
            }
            case 3 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "zh", "HK");
                if (recreate) activity.recreate();
            }
            case 4 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "ja", "JP");
                if (recreate) activity.recreate();
            }
            case 5 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "pl", "PL");
                if (recreate) activity.recreate();
            }
            case 6 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "ru", "RU");
                if (recreate) activity.recreate();
            }
            case 7 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "ar", "SA");
                if (recreate) activity.recreate();
            }
            case 8 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "es", "ES");
                if (recreate) activity.recreate();
            }
            case 9 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "pt", "BR");
                if (recreate) activity.recreate();
            }
            case 10 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "in", "ID");
                if (recreate) activity.recreate();
            }
            case 11 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "tr", "TR");
                if (recreate) activity.recreate();
            }
            case 12 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "vi", "VN");
                if (recreate) activity.recreate();
            }
            case 13 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "it", "IT");
                if (recreate) activity.recreate();
            }
            case 14 -> {
                LanguageHelper.setLanguage(activity.getBaseContext(), "zh", "ME");
                if (recreate) activity.recreate();
            }
        }
    }

    public static String getLanguage(Context context) {
        Resources resources = context.getResources();
        String country = resources.getConfiguration().getLocales().get(0).getCountry();
        if (country.isEmpty()) {
            return resources.getConfiguration().getLocales().get(0).getLanguage();
        }
        return resources.getConfiguration().getLocales().get(0).getLanguage() + "_" + country;
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
        return -1;
    }

    public static boolean isUpperCase(String string) {
        String result = string.toUpperCase();
        return result.equals(string);
    }
}
