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
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ShakeManager;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class ShakeDevice extends HomeBaseHookNew {

    private static final String STATE_LAUNCHER_ACTIVITY = "ShakeDevice.launcherActivity";
    private final String shakeMgrKey = "MIUIZER_SHAKE_MGR";

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        restoreShakeListener();

        findAndHookMethod("com.miui.home.launcher.BaseLauncher", "onResume", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                registerShakeListener((Activity) param.getThisObject());
            }
        });

        findAndHookMethod("com.miui.home.launcher.BaseLauncher", "onPause", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                unregisterShakeListener((Activity) param.getThisObject());
            }
        });
    }

    @Override
    public void initBase() {
        restoreShakeListener();

        findAndHookMethod("com.miui.home.launcher.Launcher", "onResume", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                registerShakeListener((Activity) param.getThisObject());
            }
        });

        findAndHookMethod("com.miui.home.launcher.Launcher", "onPause", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                unregisterShakeListener((Activity) param.getThisObject());
            }
        });
    }

    private void restoreShakeListener() {
        Activity activity = getHotReloadRuntimeState(STATE_LAUNCHER_ACTIVITY, Activity.class);
        if (activity != null) {
            registerShakeListener(activity);
        }
    }

    private void registerShakeListener(Activity launcherActivity) {
        Object stored = com.sevtinge.hyperceiler.libhook.base.BaseHook.getAdditionalInstanceField(
            launcherActivity, shakeMgrKey
        );
        ShakeManager shakeMgr = stored instanceof ShakeManager
            ? (ShakeManager) stored : new ShakeManager(launcherActivity);
        com.sevtinge.hyperceiler.libhook.base.BaseHook.setAdditionalInstanceField(
            launcherActivity, shakeMgrKey, shakeMgr
        );
        SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(
            Context.SENSOR_SERVICE
        );
        shakeMgr.reset();
        sensorMgr.registerListener(
            shakeMgr,
            sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        );
        registerSensorListenerHotReloadCleanup(sensorMgr, shakeMgr);
        putHotReloadRuntimeState(STATE_LAUNCHER_ACTIVITY, launcherActivity);
    }

    private void unregisterShakeListener(Activity launcherActivity) {
        Object stored = com.sevtinge.hyperceiler.libhook.base.BaseHook.getAdditionalInstanceField(
            launcherActivity, shakeMgrKey
        );
        if (stored instanceof ShakeManager shakeMgr) {
            SensorManager sensorMgr = (SensorManager) launcherActivity.getSystemService(
                Context.SENSOR_SERVICE
            );
            sensorMgr.unregisterListener(shakeMgr);
        }
        putHotReloadRuntimeState(STATE_LAUNCHER_ACTIVITY, null);
    }
}
