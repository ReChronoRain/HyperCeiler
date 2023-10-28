package com.sevtinge.hyperceiler.module.hook.home.other;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static de.robv.android.xposed.XC_MethodReplacement.returnConstant;
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

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

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
            hookAllMethods("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$MultipleSmallWindowShortcutMenuItem", "isValid", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    returnConstant(true);
                }
            });
            hookAllMethods("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$SmallWindowShortcutMenuItem", "isValid", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    returnConstant(true);
                }
            });
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

            hookAllMethods(mViewDarkModeHelper, "onConfigurationChanged", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    XposedHelpers.callStaticMethod(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems");
                }
            });

            hookAllMethods(mShortcutMenuItem, "getShortTitle", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    if (param.getResult().equals("应用信息")) {
                        param.setResult("信息");
                    }
                    if (param.getResult().equals("新建窗口")) {
                        param.setResult("多开");
                    }
                }
            });

            hookAllMethods(mActivity, "onCreate", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mContext = (Context) param.thisObject;
                }
            });

            findAndHookMethod(mAppDetailsShortcutMenuItem, "getOnClickListener", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    Resources modRes = Helpers.getModuleRes(mContext);
                    Object obj = param.thisObject;
                    CharSequence mShortTitle = (CharSequence) callMethod(obj, "getShortTitle");

                    if (mShortTitle.equals(modRes.getString(R.string.share_center))) {
                        XposedHelpers.callStaticMethod(mRecentsAndFSGestureUtils, "startWorld", mContext);
                    } else if (mShortTitle.equals(modRes.getString(R.string.floating_window))) {
                        param.setResult(getFreeformOnClickListener(obj, false));
                    } else if (mShortTitle.equals(modRes.getString(R.string.new_task))) {
                        param.setResult(getFreeformOnClickListener(obj, true));
                    }
                }
            });

            hookAllMethods(mSystemShortcutMenu, "getMaxShortcutItemCount", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    param.setResult(6);
                }
            });

            hookAllMethods(mAppShortcutMenu, "getMaxShortcutItemCount", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    param.setResult(6);
                }
            });

            hookAllMethods(mSystemShortcutMenuItem, "createAllSystemShortcutMenuItems", new MethodHook() {
                @SuppressLint("DiscouragedApi")
                @Override
                protected void after(MethodHookParam param) throws Throwable {

                    Resources modRes = Helpers.getModuleRes(mContext);

                    List mAllSystemShortcutMenuItems = (List) XposedHelpers.getStaticObjectField(mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems");

                    Object mSmallWindowInstance = XposedHelpers.newInstance(mAppDetailsShortcutMenuItem);
                    Object mNewTasksInstance = XposedHelpers.newInstance(mAppDetailsShortcutMenuItem);
                    if (mPrefsMap.getBoolean("home_other_freeform_shortcut_menu")) {
                        callMethod(mSmallWindowInstance, "setShortTitle", modRes.getString(R.string.floating_window));
                        callMethod(mSmallWindowInstance, "setIconDrawable", ContextCompat.getDrawable(mContext, mContext.getResources().getIdentifier("ic_task_small_window", "drawable", mContext.getPackageName())));
                    }
                    if (mPrefsMap.getBoolean("home_other_tasks_shortcut_menu")) {
                        callMethod(mNewTasksInstance, "setShortTitle", modRes.getString(R.string.new_task));
                        callMethod(mNewTasksInstance, "setIconDrawable", ContextCompat.getDrawable(mContext, mContext.getResources().getIdentifier("ic_task_add_pair", "drawable", mContext.getPackageName())));
                    }

                    ArrayList sAllSystemShortcutMenuItems = new ArrayList();
                    if (mPrefsMap.getBoolean("home_other_freeform_shortcut_menu"))
                        sAllSystemShortcutMenuItems.add(mSmallWindowInstance);
                    if (mPrefsMap.getBoolean("home_other_tasks_shortcut_menu"))
                        sAllSystemShortcutMenuItems.add(mNewTasksInstance);
                    sAllSystemShortcutMenuItems.addAll(mAllSystemShortcutMenuItems);
                    XposedHelpers.setStaticObjectField(mSystemShortcutMenuItem, "sAllSystemShortcutMenuItems", sAllSystemShortcutMenuItems);
                }
            });

        } catch (Throwable th) {
            logW(TAG, "FreeformShortcutMenu", th);
        }
    }


    private View.OnClickListener getFreeformOnClickListener(Object obj, boolean isNewTaskOnClick) {
        return view -> {
            Intent intent = new Intent();
            Context mContext1 = view.getContext();
            ComponentName mComponentName = (ComponentName) callMethod(obj, "getComponentName", new Object[0]);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setComponent(mComponentName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (isNewTaskOnClick) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            }
            Object makeFreeformActivityOptions = XposedHelpers.callStaticMethod(mActivityUtilsCompat, "makeFreeformActivityOptions", mContext1, mComponentName.getPackageName());

            if (makeFreeformActivityOptions != null) {
                mContext1.startActivity(intent, (Bundle) callMethod(makeFreeformActivityOptions, "toBundle", new Object[0]));
            }
        };
    }
}
