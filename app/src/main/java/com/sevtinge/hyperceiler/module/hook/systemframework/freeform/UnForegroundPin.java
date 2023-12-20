package com.sevtinge.hyperceiler.module.hook.systemframework.freeform;

import android.graphics.Rect;
import android.util.Log;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class UnForegroundPin extends BaseHook {
    @Override
    public void init() {
        try {
            /*Hyper*/
            getDeclaredMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "needForegroundPin",
                "com.android.server.wm.MiuiFreeFormActivityStack");
            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "needForegroundPin",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        } catch (Throwable throwable) {
            logE(TAG, "Hyper UnForegroundPin E, if you is Miui don't worry : " + throwable);
            /*Miui*/
            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "moveTaskToBack",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeFormManagerService",
                "updatePinFloatingWindowPos",
                Rect.class, int.class, boolean.class,
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        if ((boolean) param.args[2]) {
                            Object mffas = XposedHelpers.callMethod(param.thisObject,
                                "getMiuiFreeFormActivityStackForMiuiFB", param.args[1]);
                            XposedHelpers.callMethod(XposedHelpers.getObjectField(mffas,
                                "mLastIconLayerWindowToken"), "setVisibility", false, false);
                            Object mMiuiFreeFormGestureController = XposedHelpers.getObjectField(
                                XposedHelpers.getObjectField(
                                    XposedHelpers.getObjectField(
                                        param.thisObject,
                                        "mActivityTaskManagerService"),
                                    "mWindowManager"),
                                "mMiuiFreeFormGestureController");
                            Object mGestureAnimator = XposedHelpers.getObjectField(
                                XposedHelpers.getObjectField(mMiuiFreeFormGestureController,
                                    "mGestureListener"),
                                "mGestureAnimator");
                            XposedHelpers.callMethod(mGestureAnimator, "hideStack", mffas);
                            XposedHelpers.callMethod(mGestureAnimator, "applyTransaction");
                        }
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "moveTaskToFront",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeformPinManagerService",
                "lambda$unPinFloatingWindow$0$com-android-server-wm-MiuiFreeformPinManagerService",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                float.class, float.class, boolean.class, "com.android.server.wm.DisplayContent",
                "com.android.server.wm.MiuiFreeFormFloatIconInfo",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        Object mffas = param.args[0];
                        Object activityRecord = XposedHelpers.callMethod(XposedHelpers.getObjectField(mffas, "mTask"),
                            "getTopNonFinishingActivity");
                        /*遵循安卓日志*/
                        Log.i("MiuiFreeformPinManagerService",
                            "unPinFloatingWindow mffas: " + mffas + " activityRecord: " + activityRecord);
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

            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "lambda$startFullscreenFromFreeform$2$com-android-server-wm-MiuiFreeFormGestureController",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        Object mffas = param.args[0];
                        Object mGestureListener = XposedHelpers.getObjectField(param.thisObject, "mGestureListener");
                        if ((boolean) XposedHelpers.callMethod(mffas, "isInFreeFormMode")) {
                            XposedHelpers.callMethod(mGestureListener, "startFullScreenFromFreeFormAnimation", mffas);
                            Object mTrackManager = XposedHelpers.getObjectField(param.thisObject, "mTrackManager");
                            if (mTrackManager != null) {
                                XposedHelpers.callMethod(mTrackManager, "trackSmallWindowPinedQuitEvent",
                                    XposedHelpers.callMethod(mffas, "getStackPackageName"),
                                    XposedHelpers.callMethod(mffas, "getApplicationName"),
                                    (long) XposedHelpers.getObjectField(mffas, "mPinedStartTime") != 0 ?
                                        ((float) (System.currentTimeMillis() -
                                            (long) XposedHelpers.getObjectField(mffas,
                                                "mPinedStartTime"))) / 1000.0f : 0.0f
                                );
                            }
                        } else if ((boolean) XposedHelpers.callMethod(mffas, "isInMiniFreeFormMode")) {
                            XposedHelpers.callMethod(mGestureListener, "startFullScreenFromSmallAnimation", mffas);
                            Object mTrackManager = XposedHelpers.getObjectField(param.thisObject, "mTrackManager");
                            if (mTrackManager != null) {
                                XposedHelpers.callMethod(mTrackManager, "trackMiniWindowPinedQuitEvent",
                                    XposedHelpers.callMethod(mffas, "getStackPackageName"),
                                    XposedHelpers.callMethod(mffas, "getApplicationName"),
                                    (long) XposedHelpers.getObjectField(mffas, "mPinedStartTime") != 0 ?
                                        ((float) (System.currentTimeMillis() -
                                            (long) XposedHelpers.getObjectField(mffas,
                                                "mPinedStartTime"))) / 1000.0f : 0.0f
                                );
                            }
                        }
                        XposedHelpers.setObjectField(mffas, "mPinedStartTime", 0L);
                        XposedHelpers.callMethod(mffas, "setInPinMode", false);
                        param.setResult(null);
                    }
                }
            );

        }
    }
}
