package com.sevtinge.cemiuiler.module.hook.systemui;

import static com.sevtinge.cemiuiler.utils.log.AndroidLogUtils.LogD;

import android.view.MotionEvent;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class SwitchControlPanel extends BaseHook {

    Class<?> mControlPanelWindowManager;

    @Override
    public void init() {

        mControlPanelWindowManager = findClassIfExists("com.android.systemui.controlcenter.phone.ControlPanelWindowManager");

        findAndHookMethod(mControlPanelWindowManager, "dispatchToControlPanel", MotionEvent.class, float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                float f = (float) param.args[1];
                XposedHelpers.setFloatField(param.thisObject, "mDownX", f);
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                int i = (Float.compare(mDownX, f / 2.0f));
                LogD(TAG, "mDownX：" + mDownX + "in before");
                LogD(TAG, "f：" + f + "in before");
                LogD(TAG, "：" + i + "in before");
                i *= -1;
                int i2 = i;
                LogD(TAG, "：" + i2 + "in before");
            }

            @Override
            protected void after(MethodHookParam param) {
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                float f = (float) param.args[1];
                int i = (Float.compare(mDownX, f / 2.0f));
                LogD(TAG, "mDownX：" + mDownX + "in after");
                LogD(TAG, "f：" + f + "in after");
                LogD(TAG, "：" + i + "in after");
                i *= -1;
                int i2 = i;
                LogD(TAG, "：" + i2 + "in after");
            }
        });
    }
}
