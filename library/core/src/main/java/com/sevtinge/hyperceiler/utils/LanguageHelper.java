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
package com.sevtinge.hyperceiler.utils;

import android.app.Activity;
import android.content.Context;

import com.sevtinge.hyperceiler.common.utils.AppLanguageHelper;

import java.util.Locale;

public class LanguageHelper {
    public static final String[] APP_LANGUAGES = AppLanguageHelper.APP_LANGUAGES;

    public static void init(Context context) {
        AppLanguageHelper.init(context);
    }

    public static void applyLanguage(Context context, int index) {
        AppLanguageHelper.applyLanguage(context, index);
    }

    public static Locale getCachedLocale(String language, String country) {
        return AppLanguageHelper.getCachedLocale(language, country);
    }

    public static void init(Activity activity) {
        AppLanguageHelper.init(activity);
    }

    public static void setLanguage(Context context, String language) {
        AppLanguageHelper.setLanguage(context, language);
    }

    public static void setLanguage(Context context, String language, String country) {
        AppLanguageHelper.setLanguage(context, language, country);
    }

    public static void setLanguage(Context context, Locale locale) {
        AppLanguageHelper.setLanguage(context, locale);
    }

    public static void setIndexLanguage(Activity activity, int index, boolean recreate) {
        AppLanguageHelper.setIndexLanguage(activity, index, recreate);
    }

    public static String getLanguage(Context context) {
        return AppLanguageHelper.getLanguage(context);
    }

    public static String newLanguage(String language, String country) {
        return AppLanguageHelper.newLanguage(language, country);
    }

    public static int resultIndex(String[] languages, String value) {
        return AppLanguageHelper.resultIndex(languages, value);
    }

    public static boolean isUpperCase(String string) {
        return AppLanguageHelper.isUpperCase(string);
    }

    public static Locale localeFromAppLanguage(String lang) {
        return AppLanguageHelper.localeFromAppLanguage(lang);
    }

}
