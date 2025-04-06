/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.home.gesture;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.utils.ShakeManager;

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
