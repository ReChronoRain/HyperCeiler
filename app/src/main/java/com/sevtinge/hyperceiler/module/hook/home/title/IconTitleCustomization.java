package com.sevtinge.hyperceiler.module.hook.home.title;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedHelpers;

public class IconTitleCustomization extends BaseHook {
    private static Set<String> selectedApps = new LinkedHashSet<>(mPrefsMap.getStringSet("home_title_title_icontitlecustomization"));

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.Launcher",
            "onCreate", Bundle.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Activity act = (Activity) param.thisObject;
                    Context context = act.getBaseContext();
                    Handler handler = new Handler(context.getMainLooper());
                    // Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                    new PrefsUtils.SharedPrefsObserver(context, handler, "prefs_key_home_title_title_icontitlecustomization") {
                        @Override
                        public void onChange(String name, String defValue) {
                            try {
                                selectedApps = PrefsUtils.getSharedStringSetPrefs(context, name);
                                // selectedApps = new LinkedHashSet<>(mPrefsMap.getStringSet("home_title_title_icontitlecustomization"));
                                // logE(TAG, "haa: " + selectedApps);
                                HashSet<?> mAllLoadedApps;
                                if (XposedHelpers.findFieldIfExists(param.thisObject.getClass(), "mAllLoadedShortcut") != null)
                                    mAllLoadedApps = (HashSet<?>) XposedHelpers.getObjectField(param.thisObject, "mAllLoadedShortcut");
                                else if (XposedHelpers.findFieldIfExists(param.thisObject.getClass(), "mAllLoadedApps") != null)
                                    mAllLoadedApps = (HashSet<?>) XposedHelpers.getObjectField(param.thisObject, "mAllLoadedApps");
                                else
                                    mAllLoadedApps = (HashSet<?>) XposedHelpers.getObjectField(param.thisObject, "mLoadedAppsAndShortcut");
                                Activity act = (Activity) param.thisObject;
                                if (mAllLoadedApps != null) {
                                    for (Object shortcut : mAllLoadedApps) {
                                        boolean isApplicatoin = (boolean) XposedHelpers.callMethod(shortcut, "isApplicatoin");
                                        if (!isApplicatoin) continue;
                                        String pkgName = (String) XposedHelpers.callMethod(shortcut, "getPackageName");
                                        // String actName = (String) XposedHelpers.callMethod(shortcut, "getClassName");
                                        // UserHandle user = (UserHandle) XposedHelpers.getObjectField(shortcut, "user");
                                        CharSequence getApp = getAppName(pkgName);
                                        if (getApp != null && !getApp.equals("")) {
                                            // CharSequence newStr = TextUtils.isEmpty(newTitle) ? (CharSequence) XposedHelpers.getAdditionalInstanceField(shortcut, "mLabelOrig") : newTitle;
                                            XposedHelpers.setObjectField(shortcut, "mLabel", getApp);
                                            act.runOnUiThread(() -> {
                                                if (lpparam.packageName.equals("com.miui.home")) {
                                                    XposedHelpers.callMethod(shortcut, "updateBuddyIconView", act);
                                                } else {
                                                    Object buddyIconView = XposedHelpers.callMethod(shortcut, "getBuddyIconView");
                                                    if (buddyIconView != null)
                                                        XposedHelpers.callMethod(buddyIconView, "updateInfo", param.thisObject, shortcut);
                                                }
                                            });
                                            break;
                                        }
                                    }
                                }
                            } catch (Throwable throwable) {
                                logE(TAG, "e: " + throwable);
                            }
                        }
                    };
                }
            }
        );

        hookAllConstructors("com.miui.home.launcher.ShortcutInfo",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject,
                        "mLabelOrig", XposedHelpers.getObjectField(param.thisObject, "mLabel"));
                    if (param.args != null && param.args.length > 0)
                        modifyTitle(param.thisObject);
                }
            }
        );

        findAndHookMethodSilently("com.miui.home.launcher.ShortcutInfo",
            "loadToggleInfo", Context.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mLabelOrig",
                        XposedHelpers.getObjectField(param.thisObject, "mLabel"));
                    modifyTitle(param.thisObject);
                }
            }
        );

        findAndHookMethodSilently("com.miui.home.launcher.ShortcutInfo",
            "setLabelAndUpdateDB", CharSequence.class, Context.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject,
                        "mLabelOrig", param.args[0]);
                    modifyTitle(param.thisObject);
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ShortcutInfo",
            "load", Context.class, Cursor.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    modifyTitle(param.thisObject);
                }
            }
        );

        hookAllMethodsSilently("com.miui.home.launcher.BaseAppInfo",
            "resetTitle",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    modifyTitle(param.thisObject);
                }
            }
        );
    }

    public void modifyTitle(Object thisObject) {
        boolean isApplicatoin = (boolean) XposedHelpers.callMethod(thisObject, "isApplicatoin");
        if (!isApplicatoin) return;
        String pkgName = (String) XposedHelpers.callMethod(thisObject, "getPackageName");
        // String actName = (String) XposedHelpers.callMethod(thisObject, "getClassName");
        // UserHandle user = (UserHandle) XposedHelpers.getObjectField(thisObject, "user");
        String newTitle = (String) getAppName(pkgName);
        if (newTitle != null && !newTitle.equals(""))
            XposedHelpers.setObjectField(thisObject, "mLabel", newTitle);
    }

    public CharSequence getAppName(String packageName) {
        String string2 = "";
        Pattern pattern = Pattern.compile(".*฿(.*)฿.*");
        for (String edit : selectedApps) {
            if (edit.contains(packageName + "฿")) {
                Matcher matcher = pattern.matcher(edit);
                if (matcher.find()) {
                    string2 = matcher.group(1);
                }
            }
        }
        return string2;
    }
}
