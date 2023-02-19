package com.sevtinge.cemiuiler.module.systemui;

import android.view.MotionEvent;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;

import de.robv.android.xposed.XposedHelpers;

public class SwitchControlPanel extends BaseHook {

    Class<?> mControlPanelWindowManager;

    @Override
    public void init() {

        mControlPanelWindowManager = findClassIfExists("com.android.systemui.controlcenter.phone.ControlPanelWindowManager");

        findAndHookMethod(mControlPanelWindowManager, "dispatchToControlPanel", MotionEvent.class, float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                float f = (float) param.args[1];
                XposedHelpers.setFloatField(param.thisObject, "mDownX", f);
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                int i = (mDownX > (f / 2.0f) ? 1 : (mDownX == (f / 2.0f) ? 0 : -1));
                LogUtils.logXp(TAG, "mDownX：" + mDownX + "in before");
                LogUtils.logXp(TAG, "f：" + f + "in before");
                LogUtils.logXp(TAG, "：" + i + "in before");
                i *= -1;
                int i2 = i;
                LogUtils.logXp(TAG, "：" + i2 + "in before");
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                float f = (float) param.args[1];
                int i = (mDownX > (f / 2.0f) ? 1 : (mDownX == (f / 2.0f) ? 0 : -1));
                LogUtils.logXp(TAG, "mDownX：" + mDownX + "in after");
                LogUtils.logXp(TAG, "f：" + f + "in after");
                LogUtils.logXp(TAG, "：" + i + "in after");
                i *= -1;
                int i2 = i;
                LogUtils.logXp(TAG, "：" + i2 + "in after");
            }
        });
    }
}
