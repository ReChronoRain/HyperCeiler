package com.sevtinge.hyperceiler.module.hook.home.gesture;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.ShakeManager;

import de.robv.android.xposed.XposedHelpers;

public class ShakeDevice extends BaseHook {
    @Override
    public void init() {
        final String shakeMgrKey = "MIUIZER_SHAKE_MGR";

        findAndHookMethod("com.miui.home.launcher.Launcher", "onResume", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                ShakeManager shakeMgr = (ShakeManager) XposedHelpers.getAdditionalInstanceField(param.thisObject, shakeMgrKey);
                if (shakeMgr == null) {
                    shakeMgr = new ShakeManager((Context) param.thisObject);
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, shakeMgrKey, shakeMgr);
                }
                Activity launcherActivity = (Activity) param.thisObject;
                SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(Context.SENSOR_SERVICE);
                shakeMgr.reset();
                sensorMgr.registerListener(shakeMgr, sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        findAndHookMethod("com.miui.home.launcher.Launcher", "onPause", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                if (XposedHelpers.getAdditionalInstanceField(param.thisObject, shakeMgrKey) == null) return;
                Activity launcherActivity = (Activity) param.thisObject;
                SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(Context.SENSOR_SERVICE);
                sensorMgr.unregisterListener((ShakeManager) XposedHelpers.getAdditionalInstanceField(param.thisObject, shakeMgrKey));
            }
        });
    }
}
