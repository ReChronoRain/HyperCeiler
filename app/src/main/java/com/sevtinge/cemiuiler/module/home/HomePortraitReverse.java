package com.sevtinge.cemiuiler.module.home;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class HomePortraitReverse extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.Launcher", "onCreate", Bundle.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Activity act = (Activity)param.thisObject;
                act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        });
    }
}
