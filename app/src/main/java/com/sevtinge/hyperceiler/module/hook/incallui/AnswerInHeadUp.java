package com.sevtinge.hyperceiler.module.hook.incallui;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import miui.process.ForegroundInfo;
import miui.process.ProcessManager;

public class AnswerInHeadUp extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.incallui.InCallPresenter", "answerIncomingCall", Context.class, String.class, int.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                boolean showUi = (boolean) param.args[3];
                if (showUi) {
                    ForegroundInfo foregroundInfo = ProcessManager.getForegroundInfo();
                    if (foregroundInfo != null) {
                        String topPackage = foregroundInfo.mForegroundPackageName;
                        /*if (!"com.miui.home".equals(topPackage)) {
                            param.args[3] = false;
                        }*/
                    }
                }
            }
        });
    }
}
