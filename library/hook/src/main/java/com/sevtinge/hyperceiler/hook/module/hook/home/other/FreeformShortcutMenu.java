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
package com.sevtinge.hyperceiler.hook.module.hook.home.other;

import static com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.getModuleRes;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static de.robv.android.xposed.XposedHelpers.callMethod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.hook.R;
import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class FreeformShortcutMenu extends BaseHook {

    Class<?> mActivity;
    Class<?> mViewDarkModeHelper;
    Class<?> mSystemShortcutMenu;
    Class<?> mSystemShortcutMenuItem;
    Class<?> mAppShortcutMenu;
    Class<?> mShortcutMenuItem;
    Class<?> mAppDetailsShortcutMenuItem;
    Class<?> mActivityUtilsCompat;
    Class<?> mRecentsAndFSGestureUtils;

    Context mContext;

    XC_MethodHook.Unhook mShortCutMenuItemHook;

    @Override
    public void init() {

        if (isPad()) {
            hookAllMethods("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$SmallWindowShortcutMenuItem", "isValid",
                MethodHook.returnConstant(true));
            return;
        }

        mActivity = Activity.class;
        mViewDarkModeHelper = findClassIfExists("com.miui.home.launcher.util.ViewDarkModeHelper");
        mSystemShortcutMenu = findClassIfExists("com.miui.home.launcher.shortcuts.SystemShortcutMenu");
        mSystemShortcutMenuItem = findClassIfExists("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem");
        mAppShortcutMenu = findClassIfExists("com.miui.home.launcher.shortcuts.AppShortcutMenu");
        mShortcutMenuItem = findClassIfExists("com.miui.home.launcher.shortcuts.ShortcutMenuItem");
        mAppDetailsShortcutMenuItem = findClassIfExists("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$AppDetailsShortcutMenuItem");
        mActivityUtilsCompat = findClassIfExists("com.miui.launcher.utils.ActivityUtilsCompat");
        mRecentsAndFSGestureUtils = findClassIfExists("com.miui.home.launcher.RecentsAndFSGestureUtils");

        try {
            if (mViewDarkModeHelper != null) {
                hookAllMethods(mViewDarkModeHelper, "onConfigurationChanged", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        XposedHelpers.callStaticMethod(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems");
                    }
                });
            }

            if (mShortcutMenuItem != null) {
                hookAllMethods(mShortcutMenuItem, "getShortTitle", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        final Object result = param.getResult();
                        final String rs = String.valueOf(result);
                        if ("应用信息".equals(rs)) {
                            param.setResult("信息");
                        } else if ("新建窗口".equals(rs)) {
                            param.setResult("多开");
                        }
                    }
                });
            }

            hookAllMethods(mActivity, "onCreate", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mContext = (Context) param.thisObject;
                }
            });

            if (mAppDetailsShortcutMenuItem != null) {
                findAndHookMethod(mAppDetailsShortcutMenuItem, "getOnClickListener", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        if (mContext == null) return;
                        final Resources modRes = getModuleRes(mContext);
                        final Object obj = param.thisObject;
                        final CharSequence mShortTitle = (CharSequence) callMethod(obj, "getShortTitle");

                        if (mShortTitle != null && mShortTitle.toString().contentEquals(modRes.getString(R.string.share_center))) {
                            XposedHelpers.callStaticMethod(mRecentsAndFSGestureUtils, "startWorld", mContext);
                        } else if (mShortTitle != null && mShortTitle.toString().contentEquals(modRes.getString(R.string.floating_window))) {
                            param.setResult(getFreeformOnClickListener(obj, false));
                        } else if (mShortTitle != null && mShortTitle.toString().contentEquals(modRes.getString(R.string.new_task))) {
                            param.setResult(getFreeformOnClickListener(obj, true));
                        }
                    }
                });
            }

            if (mSystemShortcutMenu != null) {
                hookAllMethods(mSystemShortcutMenu, "getMaxShortcutItemCount", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        param.setResult(6);
                    }
                });
            }

            if (mAppShortcutMenu != null) {
                hookAllMethods(mAppShortcutMenu, "getMaxShortcutItemCount", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        param.setResult(6);
                    }
                });
            }

            if (mSystemShortcutMenuItem != null) {
                hookAllMethods(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems", new MethodHook() {
                    @SuppressLint("DiscouragedApi")
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {

                        if (mContext == null) return;
                        final Resources modRes = getModuleRes(mContext);

                        final List<Object> mAllSystemShortcutMenuItems = (List<Object>) XposedHelpers.getStaticObjectField(mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems");

                        final Object mSmallWindowInstance = XposedHelpers.newInstance(mAppDetailsShortcutMenuItem);
                        final Object mNewTasksInstance = XposedHelpers.newInstance(mAppDetailsShortcutMenuItem);

                        if (mPrefsMap.getBoolean("home_other_freeform_shortcut_menu")) {
                            callMethod(mSmallWindowInstance, "setShortTitle", modRes.getString(R.string.floating_window));
                            callMethod(mSmallWindowInstance, "setIconDrawable", ContextCompat.getDrawable(mContext, mContext.getResources().getIdentifier("ic_task_small_window", "drawable", mContext.getPackageName())));
                        }
                        if (mPrefsMap.getBoolean("home_other_tasks_shortcut_menu")) {
                            callMethod(mNewTasksInstance, "setShortTitle", modRes.getString(R.string.new_task));
                            callMethod(mNewTasksInstance, "setIconDrawable", ContextCompat.getDrawable(mContext, mContext.getResources().getIdentifier("ic_task_add_pair", "drawable", mContext.getPackageName())));
                        }

                        final ArrayList<Object> sAllSystemShortcutMenuItems = new ArrayList<>();
                        if (mPrefsMap.getBoolean("home_other_freeform_shortcut_menu")) {
                            sAllSystemShortcutMenuItems.add(mSmallWindowInstance);
                        }
                        if (mPrefsMap.getBoolean("home_other_tasks_shortcut_menu")) {
                            sAllSystemShortcutMenuItems.add(mNewTasksInstance);
                        }
                        if (mAllSystemShortcutMenuItems != null) {
                            sAllSystemShortcutMenuItems.addAll(mAllSystemShortcutMenuItems);
                        }
                        XposedHelpers.setStaticObjectField(mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems", sAllSystemShortcutMenuItems);
                    }
                });
            }

        } catch (Throwable th) {
            logW(TAG, "FreeformShortcutMenu", th);
        }
    }


    private View.OnClickListener getFreeformOnClickListener(final Object obj, final boolean isNewTaskOnClick) {
        return view -> {
            final Intent intent = new Intent();
            final Context ctx = view.getContext();
            final ComponentName componentName = (ComponentName) callMethod(obj, "getComponentName", new Object[0]);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setComponent(componentName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (isNewTaskOnClick) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            }
            final Object makeFreeformActivityOptions = XposedHelpers.callStaticMethod(mActivityUtilsCompat, "makeFreeformActivityOptions", ctx, componentName.getPackageName());

            if (makeFreeformActivityOptions != null) {
                ctx.startActivity(intent, (Bundle) callMethod(makeFreeformActivityOptions, "toBundle", new Object[0]));
            }
        };
    }
}
