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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Bundle;

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.lang.reflect.Field;
import java.util.Locale;

public class LocaleActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private volatile Locale currentLocale;

    public LocaleActivityLifecycleCallbacks(Locale initialLocale) {
        this.currentLocale = initialLocale;
    }

    public void setLocale(Locale locale) {
        if (locale != null) this.currentLocale = locale;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        applyLocaleToActivity(activity, currentLocale);
    }

    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityResumed(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
    @Override public void onActivityDestroyed(Activity activity) { }

    private void applyLocaleToActivity(Activity activity, Locale locale) {
        if (activity == null || locale == null) return;
        try {
            Context base = activity.getBaseContext();
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.setLocale(locale);

            Context localized = base.createConfigurationContext(config);

            Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
            mBaseField.setAccessible(true);
            mBaseField.set(activity, localized);
            AndroidLogUtils.logI("Language", "Applied locale " + locale + " to activity " + activity.getClass().getSimpleName());
        } catch (Throwable t) {
            AndroidLogUtils.logE("Language", "Failed to apply locale to activity: " + activity.getClass().getName(), t);
        }
    }
}
