package com.sevtinge.cemiuiler.module.hook.systemui;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidU;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class HideNavigationBar extends BaseHook {

    @Override
    public void init() {
        if (isAndroidU()) {
            hookAllConstructors("com.android.systemui.statusbar.phone.NavigationModeControllerExt", new MethodHook() {
                @SuppressLint("PrivateApi")
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    super.after(param);
                    XposedHelpers.setStaticBooleanField(param.getClass().getClassLoader().loadClass("com.android.systemui.statusbar.phone.NavigationModeControllerExt"), "mHideGestureLine", true);
                }
            });
        } else {
            findAndHookMethod("com.android.systemui.statusbar.phone.NavigationModeControllerExt",
                "hideNavigationBar",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        }

        hookAllMethods("com.android.systemui.navigationbar.NavigationBarController",
            "createNavigationBar",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    if (param.args.length >= 3) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        try {
                            int End = Settings.Global.getInt(mContext.getContentResolver(), "hide_gesture_line");
                            if (End == 1) {
                                Settings.Global.putInt(mContext.getContentResolver(), "hide_gesture_line", 0);
                                logI("Settings The hide_gesture_line To 0");
                            }
                        } catch (Settings.SettingNotFoundException e) {
                            logI("Don‘t Have hide_gesture_line");
                        }
                        param.setResult(null);
                    }
                }
            }
        );
        findAndHookMethod("com.android.systemui.statusbar.phone.MiuiDockIndicatorService",
            "onNavigationModeChanged", int.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    XposedHelpers.setObjectField(param.thisObject, "mNavMode", param.args[0]);
                    if (XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView") != null) {
                        XposedHelpers.callMethod(param.thisObject, "setNavigationBarView", (Object) null);
                    } else {
                        XposedHelpers.callMethod(param.thisObject, "checkAndApplyNavigationMode");
                    }
                    param.setResult(null);
                }
            }
        );
    }

}
