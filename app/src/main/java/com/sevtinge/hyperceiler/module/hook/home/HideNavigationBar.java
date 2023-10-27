package com.sevtinge.hyperceiler.module.hook.home;

import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class HideNavigationBar extends BaseHook {
    @Override
    public void init() {
        /*横屏隐藏*/
        findAndHookMethod("com.miui.home.recents.views.RecentsContainer",
            "showLandscapeOverviewGestureView",
            boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.args[0] = false;
                }
            }
        );

        /*锁定返回*/
        findAndHookMethod("com.miui.home.recents.NavStubView",
            "isMistakeTouch", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    View navView = (View) param.thisObject;
                    boolean misTouch = false;
                    boolean setting = Settings.Global.getInt(navView.getContext().getContentResolver(), "show_mistake_touch_toast", 1) != 0;
                    if (setting) {
                        boolean mIsShowStatusBar = XposedHelpers.getBooleanField(param.thisObject, "mIsShowStatusBar");
                        if (!mIsShowStatusBar) {
                            misTouch = (boolean) XposedHelpers.callMethod(param.thisObject, "isLandScapeActually");
                        }
                    }
                    param.setResult(misTouch);
                }
            }
        );

        /*横屏设置状态*/
        findAndHookMethod("com.miui.home.recents.NavStubView", "onPointerEvent",
            MotionEvent.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    boolean mIsInFsMode = XposedHelpers.getBooleanField(param.thisObject, "mIsInFsMode");
                    if (!mIsInFsMode) {
                        MotionEvent motionEvent = (MotionEvent) param.args[0];
                        if (motionEvent.getAction() == 0) {
                            XposedHelpers.setObjectField(param.thisObject, "mHideGestureLine", true);
                        }
                    }
                }
            }
        );

        /*恢复状态*/
        findAndHookMethod("com.miui.home.recents.NavStubView", "updateScreenSize",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    XposedHelpers.setObjectField(param.thisObject, "mHideGestureLine", false);
                }
            }
        );

    }
}
