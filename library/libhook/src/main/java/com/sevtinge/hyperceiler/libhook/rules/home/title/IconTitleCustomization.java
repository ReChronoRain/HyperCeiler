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
package com.sevtinge.hyperceiler.libhook.rules.home.title;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findFieldIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setAdditionalInstanceField;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefType;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsChangeObserver;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class IconTitleCustomization extends HomeBaseHookNew {

    private static final String PREF_KEY = "prefs_key_home_title_title_icontitlecustomization";
    private static final String PREF_SET_KEY = "home_title_title_icontitlecustomization";
    private static final Pattern TITLE_PATTERN = Pattern.compile(".*฿(.*)฿.*");

    private static final String[] LOADED_APPS_FIELDS = {
        "mAllLoadedShortcut",
        "mAllLoadedApps",
        "mLoadedAppsAndShortcut"
    };

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        hookLauncherOnCreate("com.miui.home.launcher.BaseLauncher");
        hookShortcutInfo();
    }

    @Override
    public void initBase() {
        hookLauncherOnCreate("com.miui.home.launcher.Launcher");
        hookShortcutInfo();
    }

    private void hookLauncherOnCreate(String className) {
        findAndHookMethod(className, "onCreate", Bundle.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Activity activity = (Activity) param.getThisObject();
                    setupPrefsObserver(activity, param.getThisObject());
                }
            });
    }

    private void setupPrefsObserver(Activity activity, Object launcher) {
        Context context = activity.getBaseContext();
        Handler handler = new Handler(context.getMainLooper());

        new PrefsChangeObserver(context, handler, true, PREF_KEY) {
            @Override
            public void onChange(PrefType type, Uri uri, String name, Object def) {
                try {
                    updateAppTitles(activity, launcher);
                } catch (Throwable t) {
                    XposedLog.e(TAG, getPackageName(), "onChange error", t);
                }
            }
        };
    }

    private void updateAppTitles(Activity activity, Object launcher) {
        HashSet<?> loadedApps = findLoadedApps(launcher);
        if (loadedApps == null) return;

        for (Object shortcut : loadedApps) {
            if (isApplication(shortcut)) continue;

            String pkgName = (String) callMethod(shortcut, "getPackageName");
            CharSequence newTitle = getAppName(pkgName);

            if (newTitle == null || newTitle.isEmpty()) continue;

            setObjectField(shortcut, "mLabel", newTitle);
            updateIconView(activity, launcher, shortcut);break;
        }
    }

    private HashSet<?> findLoadedApps(Object launcher) {
        Class<?> launcherClass = launcher.getClass();

        for (String fieldName : LOADED_APPS_FIELDS) {
            if (findFieldIfExists(launcherClass, fieldName) != null) {
                return (HashSet<?>) getObjectField(launcher, fieldName);
            }
        }
        return null;
    }

    private boolean isApplication(Object shortcut) {
        return !((boolean) callMethod(shortcut, "isApplicatoin"));
    }

    private void updateIconView(Activity activity, Object launcher, Object shortcut) {
        activity.runOnUiThread(() -> {
            if ("com.miui.home".equals(getPackageName())) {
                callMethod(shortcut, "updateBuddyIconView", activity);
            } else {
                Object buddyIconView = callMethod(shortcut, "getBuddyIconView");
                if (buddyIconView != null) {
                    callMethod(buddyIconView, "updateInfo", launcher, shortcut);
                }
            }
        });
    }

    private void hookShortcutInfo() {
        // Hook 构造器
        hookAllConstructors("com.miui.home.launcher.ShortcutInfo",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    saveOriginalLabel(param.getThisObject());
                    if (param.getArgs() != null && param.getArgs().length > 0) {
                        modifyTitle(param.getThisObject());
                    }
                }
            });

        // Hook loadToggleInfo
        findAndHookMethod("com.miui.home.launcher.ShortcutInfo",
            "loadToggleInfo", Context.class,
            new ShortcutInfoHook());

        // Hook setLabelAndUpdateDB
        findAndHookMethod("com.miui.home.launcher.ShortcutInfo",
            "setLabelAndUpdateDB", CharSequence.class, Context.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    setAdditionalInstanceField(param.getThisObject(), "mLabelOrig", param.getArgs()[0]);
                    modifyTitle(param.getThisObject());
                }
            });

        // Hook load
        findAndHookMethod("com.miui.home.launcher.ShortcutInfo",
            "load", Context.class, Cursor.class,
            new ModifyTitleHook());

        // Hook resetTitle
        hookAllMethods("com.miui.home.launcher.BaseAppInfo",
            "resetTitle",
            new ModifyTitleHook());
    }

    private void saveOriginalLabel(Object shortcut) {
        Object label = getObjectField(shortcut, "mLabel");
        setAdditionalInstanceField(shortcut, "mLabelOrig", label);
    }

    //复用的Hook 回调
    private class ShortcutInfoHook implements IMethodHook {
        @Override
        public void after(AfterHookParam param) {
            saveOriginalLabel(param.getThisObject());
            modifyTitle(param.getThisObject());
        }
    }

    private class ModifyTitleHook implements IMethodHook {
        @Override
        public void after(AfterHookParam param) {
            modifyTitle(param.getThisObject());
        }
    }

    private void modifyTitle(Object shortcut) {
        if (isApplication(shortcut)) return;

        String pkgName = (String) callMethod(shortcut, "getPackageName");
        String newTitle = (String) getAppName(pkgName);

        if (newTitle != null && !newTitle.isEmpty()) {
            setObjectField(shortcut, "mLabel", newTitle);
        }
    }

    private CharSequence getAppName(String packageName) {
        if (packageName == null) return "";

        String prefix = packageName + "฿";
        Set<String> customTitles = PrefsBridge.getStringSet(PREF_SET_KEY);

        if (customTitles == null) return "";

        for (String entry : customTitles) {
            if (entry.contains(prefix)) {
                Matcher matcher = TITLE_PATTERN.matcher(entry);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return "";
    }
}
