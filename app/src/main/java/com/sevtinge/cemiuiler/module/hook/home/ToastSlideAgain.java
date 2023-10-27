package com.sevtinge.cemiuiler.module.hook.home;

import android.view.MotionEvent;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ToastSlideAgain extends BaseHook {
    public XC_MethodHook.Unhook unhook = null;

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.NavStubView",
            "onPointerEvent", MotionEvent.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    unhook = hookToast();
                    // XposedLogUtils.logI("im hook onPointerEvent");
                }

                @Override
                protected void after(MethodHookParam param) {
                    unHook(unhook);
                }
            }
        );

        findAndHookMethod("com.miui.home.recents.GestureModeApp",
            "onStartGesture", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    unhook = hookToast();
                    // XposedLogUtils.logI("im hook onStartGesture");
                }

                @Override
                protected void after(MethodHookParam param) {
                    unHook(unhook);
                }
            }
        );
    }

    public XC_MethodHook.Unhook hookToast() {
        return XposedHelpers.findAndHookMethod(findClassIfExists("android.widget.Toast"),
            "show", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(null);
                    // XposedLogUtils.logI("im hook Toast show");
                }
            }
        );
    }

    public void unHook(XC_MethodHook.Unhook unhook) {
        if (unhook != null) {
            unhook.unhook();
            // XposedLogUtils.logI("the unhook is: " + unhook);
        }  // logE("the unhook is: null");

    }
}
