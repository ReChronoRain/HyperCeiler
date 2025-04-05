package com.sevtinge.hyperceiler.module.hook.systemui;

import android.annotation.SuppressLint;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Field;

public class FuckStatusbarGestures extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        hookAllConstructors("com.android.systemui.statusbar.phone.CentralSurfacesImpl", new MethodHook() {
            @SuppressLint({"PrivateApi", "SdCardPath"})
            @Override
            public void afterHookedMethod(MethodHookParam param) {
                try {
                    Object mGestureRec = param.thisObject
                            .getClass()
                            .getDeclaredField("mGestureRec")
                            .get(param.thisObject);
                    if (mGestureRec == null) return;
                    Field mFieldLogfile = mGestureRec
                            .getClass()
                            .getDeclaredField("mLogfile");
                    String mLogfile = (String) mFieldLogfile.get(mGestureRec);
                    if (mLogfile == null) return;
                    String mLogfileDir = mLogfile.substring(0, mLogfile.lastIndexOf('/'));
                    if ("/sdcard".equals(mLogfileDir)) {
                        mFieldLogfile.set(mGestureRec, "/sdcard/MIUI/" + mLogfile.substring(mLogfile.lastIndexOf('/') + 1));
                    }
                } catch (Throwable e) {
                    logE(TAG, lpparam.packageName, e);
                }
            }
        });
    }
}
