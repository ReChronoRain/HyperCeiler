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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ShakeManager;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class ShakeDevice extends HomeBaseHookNew {

    private final String shakeMgrKey = "MIUIZER_SHAKE_MGR";

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {

        findAndHookMethod("com.miui.home.launcher.BaseLauncher", "onResume", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ShakeManager shakeMgr = (ShakeManager) EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey);
                if (shakeMgr == null) {
                    shakeMgr = new ShakeManager((Context) param.getThisObject());
                    EzxHelpUtils.setAdditionalInstanceField(param.getThisObject(), shakeMgrKey, shakeMgr);
                }
                Activity launcherActivity = (Activity) param.getThisObject();
                SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(Context.SENSOR_SERVICE);
                shakeMgr.reset();
                sensorMgr.registerListener(shakeMgr, sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        findAndHookMethod("com.miui.home.launcher.BaseLauncher", "onPause", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey) == null) return;
                Activity launcherActivity = (Activity) param.getThisObject();
                SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(Context.SENSOR_SERVICE);
                sensorMgr.unregisterListener((ShakeManager) EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey));
            }
        });
    }

    @Override
    public void initBase() {

        findAndHookMethod("com.miui.home.launcher.Launcher", "onResume", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ShakeManager shakeMgr = (ShakeManager) EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey);
                if (shakeMgr == null) {
                    shakeMgr = new ShakeManager((Context) param.getThisObject());
                    EzxHelpUtils.setAdditionalInstanceField(param.getThisObject(), shakeMgrKey, shakeMgr);
                }
                Activity launcherActivity = (Activity) param.getThisObject();
                SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(Context.SENSOR_SERVICE);
                shakeMgr.reset();
                sensorMgr.registerListener(shakeMgr, sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        findAndHookMethod("com.miui.home.launcher.Launcher", "onPause", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey) == null) return;
                Activity launcherActivity = (Activity) param.getThisObject();
                SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(Context.SENSOR_SERVICE);
                sensorMgr.unregisterListener((ShakeManager) EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), shakeMgrKey));
            }
        });
    }
}
