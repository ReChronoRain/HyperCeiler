package com.sevtinge.hyperceiler.module.hook.systemframework.freeform;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import de.robv.android.xposed.XposedHelpers;

public class UnForegroundPin extends BaseHook {
    @Override
    public void init() {
        try {
            findClassIfExists("com.android.server.wm.MiuiFreeFormGestureController")
                .getDeclaredMethod("needForegroundPin");
            /*Hyper*/
            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "needForegroundPin",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        } catch (Throwable throwable) {
            logE(TAG, "Hyper UnForegroundPin E, if you is Miui don't worry : " + throwable);
            /*Miui*/
            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "moveTaskToBack",
                "com.android.server.wm.MiuiFreeFormActivityStack", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController"
                , "moveTaskToFront",
                "com.android.server.wm.MiuiFreeFormActivityStack", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeformPinManagerService",
                "lambda$unPinFloatingWindow$0$com-android-server-wm-MiuiFreeformPinManagerService",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                float.class, float.class, boolean.class, "com.android.server.wm.DisplayContent",
                "com.android.server.wm.MiuiFreeFormFloatIconInfo", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object mffas = param.args[0];
                        Object activityRecord = XposedHelpers.callMethod(XposedHelpers.getObjectField(mffas, "mTask"),
                            "getTopNonFinishingActivity");
                        /*遵循安卓日志*/
                        AndroidLogUtils.LogI("MiuiFreeformPinManagerService", "unPinFloatingWindow mffas: " + mffas + " activityRecord: " + activityRecord);
                        if (activityRecord == null) {
                            param.setResult(null);
                            return;
                        }
                        XposedHelpers.setObjectField(mffas, "mEnterVelocityX", param.args[1]);
                        XposedHelpers.setObjectField(mffas, "mEnterVelocityY", param.args[2]);
                        XposedHelpers.setObjectField(mffas, "mIsEnterClick", param.args[3]);
                        XposedHelpers.setObjectField(mffas, "mIsPinFloatingWindowPosInit", false);
                        XposedHelpers.callMethod(param.thisObject, "setUpMiuiFreeWindowFloatIconAnimation",
                            mffas, activityRecord, param.args[4], param.args[5]);
                        XposedHelpers.callMethod(param.thisObject, "startUnPinAnimation", mffas);
                        param.setResult(null);
                    }
                }
            );
        }
    }
}
