package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;

public class SwitchCCAndNotification extends BaseHook {
    @Override
    public void init() {
        Helpers.findAndHookMethod(
            "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader,
            "handleEvent",
            MotionEvent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                boolean useCC = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mPanelController"), "isExpandable");
                if (useCC) {
                    FrameLayout bar = (FrameLayout) param.thisObject;
                    Object mControlPanelWindowManager = XposedHelpers.getObjectField(param.thisObject, "mControlPanelWindowManager");
                    boolean dispatchToControlPanel = (boolean) XposedHelpers.callMethod(mControlPanelWindowManager, "dispatchToControlPanel", param.args[0], bar.getWidth());
                    XposedHelpers.callMethod(mControlPanelWindowManager, "setTransToControlPanel", dispatchToControlPanel);
                    param.setResult(dispatchToControlPanel);
                    return;
                }
                param.setResult(false);
            }
        });

        Helpers.findAndHookMethod(
            "com.android.systemui.controlcenter.phone.ControlPanelWindowManager", lpparam.classLoader,
            "dispatchToControlPanel",
            MotionEvent.class, float.class,
            new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                boolean added = XposedHelpers.getBooleanField(param.thisObject, "added");
                if (added) {
                    boolean useCC;
                    if (isMoreAndroidVersion(33)) {
                        useCC = XposedHelpers.getBooleanField(XposedHelpers.getObjectField(param.thisObject, "mControlCenterController"), "useControlCenter");
                    } else {
                        useCC = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mControlCenterController"), "isExpandable");
                    }
                    if (useCC) {
                        MotionEvent motionEvent = (MotionEvent) param.args[0];
                        if (motionEvent.getActionMasked() == 0) {
                            XposedHelpers.setObjectField(param.thisObject, "mDownX", motionEvent.getRawX());
                        }
                        Object controlCenterWindowView = XposedHelpers.getObjectField(param.thisObject, "mControlPanel");
                        if (controlCenterWindowView == null) {
                            param.setResult(false);
                        }
                        else {
                            float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                            float width = (float) param.args[1];
                            if (mDownX < width / 2.0f) {
                                param.setResult(XposedHelpers.callMethod(controlCenterWindowView, "handleMotionEvent", motionEvent, true));
                            } else {
                                param.setResult(false);
                            }
                        }
                        return;
                    }
                }
                param.setResult(false);
            }
        });
    }
}
