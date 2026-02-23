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
package com.sevtinge.hyperceiler.libhook.rules.home.other;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.isDarkMode;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.util.ArrayList;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class FreeformShortcutMenu extends BaseHook {

    private static final String[] SHORTCUT_CLASSES = {
        "com.miui.home.launcher.util.ViewDarkModeHelper",
        "com.miui.home.launcher.shortcuts.SystemShortcutMenu",
        "com.miui.home.launcher.shortcuts.SystemShortcutMenuItem",
        "com.miui.home.launcher.shortcuts.AppShortcutMenu",
        "com.miui.home.launcher.shortcuts.ShortcutMenuItem",
        "com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$AppDetailsShortcutMenuItem",
        "com.miui.launcher.utils.ActivityUtilsCompat",
        "com.miui.home.launcher.RecentsAndFSGestureUtils"
    };

    private Context mContext;
    private Class<?> mSystemShortcutMenuItem;
    private Class<?> mAppDetailsShortcutMenuItem;
    private Class<?> mActivityUtilsCompat;
    private Class<?> mRecentsAndFSGestureUtils;

    @Override
    public void init() {
        if (!loadClasses()) return;

        hookDarkModeChange();
        hookShortcutTitle();
        hookActivityOnCreate();
        hookAppDetailsClick();
        hookMaxShortcutCount();
        hookCreateSystemShortcuts();
    }

    private boolean loadClasses() {
        try {
            mSystemShortcutMenuItem = findClass(SHORTCUT_CLASSES[2]);
            mAppDetailsShortcutMenuItem = findClass(SHORTCUT_CLASSES[5]);
            mActivityUtilsCompat = findClass(SHORTCUT_CLASSES[6]);
            mRecentsAndFSGestureUtils = findClass(SHORTCUT_CLASSES[7]);
            return true;
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "Failed to load classes", t);
            return false;
        }
    }

    private void hookDarkModeChange() {
        Class<?> darkModeHelper = findClassIfExists(SHORTCUT_CLASSES[0]);
        if (darkModeHelper == null) return;

        hookAllMethods(darkModeHelper, "onConfigurationChanged",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    callStaticMethod(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems");
                }
            });
    }

    private void hookShortcutTitle() {
        Class<?> shortcutMenuItem = findClassIfExists(SHORTCUT_CLASSES[4]);
        if (shortcutMenuItem == null) return;

        hookAllMethods(shortcutMenuItem, "getShortTitle",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    String title = String.valueOf(param.getResult());
                    if ("应用信息".equals(title)) {
                        param.setResult("信息");
                    } else if ("新建窗口".equals(title)) {
                        param.setResult("多开");
                    }
                }
            });
    }

    private void hookActivityOnCreate() {
        hookAllMethods(Activity.class, "onCreate",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mContext = (Context) param.getThisObject();
                }
            });
    }

    private void hookAppDetailsClick() {
        if (mAppDetailsShortcutMenuItem == null) return;

        findAndHookMethod(mAppDetailsShortcutMenuItem, "getOnClickListener",
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (mContext == null) return;

                    Resources modRes = getModuleRes(mContext);
                    Object shortcut = param.getThisObject();
                    CharSequence title = (CharSequence) callMethod(shortcut, "getShortTitle");

                    if (title == null) return;

                    String titleStr = title.toString();
                    if (titleStr.contentEquals(modRes.getString(R.string.share_center))) {
                        callStaticMethod(mRecentsAndFSGestureUtils, "startWorld", mContext);
                    } else if (titleStr.contentEquals(modRes.getString(R.string.floating_window))) {
                        param.setResult(createFreeformClickListener(shortcut, false));
                    } else if (titleStr.contentEquals(modRes.getString(R.string.new_task))) {
                        param.setResult(createFreeformClickListener(shortcut, true));
                    }
                }
            });
    }

    private void hookMaxShortcutCount() {
        Class<?> systemMenu = findClassIfExists(SHORTCUT_CLASSES[1]);
        Class<?> appMenu = findClassIfExists(SHORTCUT_CLASSES[3]);

        IMethodHook hook = new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                param.setResult(6);
            }
        };

        if (systemMenu != null) {
            hookAllMethods(systemMenu, "getMaxShortcutItemCount", hook);
        }
        if (appMenu != null) {
            hookAllMethods(appMenu, "getMaxShortcutItemCount", hook);
        }
    }

    private void hookCreateSystemShortcuts() {
        if (mSystemShortcutMenuItem == null) return;

        hookAllMethods(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    if (mContext == null) return;
                    addCustomShortcuts();
                }
            });
    }

    @SuppressWarnings("unchecked")
    private void addCustomShortcuts() {
        List<Object> existingItems = (List<Object>) getStaticObjectField(
            mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems");

        ArrayList<Object> newItems = new ArrayList<>();

        if (PrefsBridge.getBoolean("home_other_freeform_shortcut_menu")) {
            Object smallWindow = createShortcutItem(
                R.string.floating_window, "ic_task_small_window");
            if (smallWindow != null) {
                newItems.add(smallWindow);
            }
        }

        if (PrefsBridge.getBoolean("home_other_tasks_shortcut_menu")) {
            Object newTask = createShortcutItem(
                R.string.new_task, "ic_task_add_pair");
            if (newTask != null) {
                newItems.add(newTask);
            }
        }

        if (existingItems != null) {
            newItems.addAll(existingItems);
        }

        setStaticObjectField(mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems", newItems);
    }

    @SuppressLint("DiscouragedApi")
    private Object createShortcutItem(int titleResId, String drawableName) {
        try {
            Resources modRes = getModuleRes(mContext);
            Object item = newInstance(mAppDetailsShortcutMenuItem);

            callMethod(item, "setShortTitle", modRes.getString(titleResId));

            int resId = modRes.getIdentifier(drawableName, "drawable", ProjectApi.mAppModulePkg);
            Drawable icon = resId != 0 ? ContextCompat.getDrawable(mContext, resId) : null;

            if (icon != null) {
                Drawable wrapped = DrawableCompat.wrap(icon).mutate();
                int tint = isDarkMode() ? Color.WHITE : Color.BLACK;
                DrawableCompat.setTint(wrapped, tint);
                callMethod(item, "setIconDrawable", wrapped);
            }

            return item;
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "createShortcutItem failed", t);
            return null;
        }
    }

    private View.OnClickListener createFreeformClickListener(Object shortcut, boolean isMultiTask) {
        return view -> {
            try {
                ComponentName component = (ComponentName) callMethod(shortcut, "getComponentName");
                Intent intent = createFreeformIntent(component, isMultiTask);
                Object options = callStaticMethod(mActivityUtilsCompat,
                    "makeFreeformActivityOptions",
                    view.getContext(),
                    component.getPackageName());

                if (options != null) {
                    Bundle bundle = (Bundle) callMethod(options, "toBundle");
                    view.getContext().startActivity(intent, bundle);
                }
            } catch (Throwable t) {
                XposedLog.e(TAG, getPackageName(), "startFreeform failed", t);
            }
        };
    }

    private Intent createFreeformIntent(ComponentName component, boolean isMultiTask) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setComponent(component);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (isMultiTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }

        return intent;
    }
}

