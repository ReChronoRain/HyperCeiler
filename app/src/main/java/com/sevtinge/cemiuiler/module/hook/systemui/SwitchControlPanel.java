package com.sevtinge.cemiuiler.module.hook.systemui;

import static com.sevtinge.cemiuiler.utils.log.AndroidLogUtils.LogD;
import static com.sevtinge.cemiuiler.utils.log.AndroidLogUtils.LogI;

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
                LogI(TAG, "mDownX：" + mDownX + "in before");
                LogI(TAG, "f：" + f + "in before");
                LogI(TAG, "：" + i + "in before");
                i *= -1;
                int i2 = i;
                LogI(TAG, "：" + i2 + "in before");
            }

            @Override
            protected void after(MethodHookParam param) {
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                float f = (float) param.args[1];
                int i = (Float.compare(mDownX, f / 2.0f));
                LogI(TAG, "mDownX：" + mDownX + "in after");
                LogI(TAG, "f：" + f + "in after");
                LogI(TAG, "：" + i + "in after");
                i *= -1;
                int i2 = i;
                LogI(TAG, "：" + i2 + "in after");
            }
        });
    }
}
