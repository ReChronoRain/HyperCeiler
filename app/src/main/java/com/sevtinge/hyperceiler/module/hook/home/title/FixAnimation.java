package com.sevtinge.hyperceiler.module.hook.home.title;

import android.view.MotionEvent;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;
import kotlin.jvm.internal.Intrinsics;

// from MIUI-EXTRA by Art-Chen

public class FixAnimation extends BaseHook {

    private Object mAppToHomeAnim2Bak;

    private final Runnable mRunnable = () -> {
    };

    public final Object getMAppToHomeAnim2Bak() {
        return mAppToHomeAnim2Bak;
    }

    public final void setMAppToHomeAnim2Bak(Object obj) {
        mAppToHomeAnim2Bak = obj;
    }

    public final Runnable getMRunnable() {
        return mRunnable;
    }

    @Override
    public void init() {

        Intrinsics.checkNotNull(lpparam);

        findAndHookMethod("com.miui.home.recents.NavStubView", "onInputConsumerEvent", MotionEvent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                Intrinsics.checkNotNullParameter(param, "param");
                setMAppToHomeAnim2Bak(XposedHelpers.getObjectField(param.thisObject, "mAppToHomeAnim2"));
                if (getMAppToHomeAnim2Bak() != null) {
                    XposedHelpers.setObjectField(param.thisObject, "mAppToHomeAnim2", (Object) null);
                }
            }

            @Override
            protected void after(MethodHookParam param) {
                Intrinsics.checkNotNullParameter(param, "param");
                Object obj = param.args[0];
                Intrinsics.checkNotNull(obj, "null cannot be cast to non-null type android.view.MotionEvent");
                MotionEvent motionEvent = (MotionEvent) obj;
                logI(TAG, FixAnimation.this.lpparam.packageName, "onInputConsumerEvent: Action: " + motionEvent.getAction() + ", return " + param.getResult() + ". x: " + motionEvent.getX() + " y: " + motionEvent.getY());
                if (XposedHelpers.getObjectField(param.thisObject, "mAppToHomeAnim2") != null || getMAppToHomeAnim2Bak() == null) {
                    return;
                }
                XposedHelpers.setObjectField(param.thisObject, "mAppToHomeAnim2", getMAppToHomeAnim2Bak());
            }

        });

        findAndHookMethod("com.miui.home.launcher.ItemIcon", "initPerformClickRunnable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Intrinsics.checkNotNullParameter(param, "param");
                param.setResult(getMRunnable());
            }

        });
    }
}
