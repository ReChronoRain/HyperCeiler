/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.PowerManager;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class BackTapWhenLocked extends BaseHook {
    private static final String TAG = "BackTapWhenLocked";
    private static final int BACKTAP_SENSOR_TYPE = 33171045;
    private static final int FLAG_ACTIVITY_SHOW_WHEN_LOCKED = 0x00080000;
    private static final int LAUNCH_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK
        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        | FLAG_ACTIVITY_SHOW_WHEN_LOCKED;

    @Override
    public void init() {
        // Replace non-wakeup dbtap sensor with the wakeup variant so that taps
        // can wake the SoC while the screen is off.
        findAndHookConstructor("miui.util.BackTapSensorWrapper", android.content.Context.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                if (!PrefsBridge.getBoolean("system_framework_back_tap_when_locked")) return;
                try {
                    Object wrapper = param.getThisObject();
                    SensorManager sm = (SensorManager) getObjectField(wrapper, "mSensorManager");
                    Sensor wakeup = sm.getDefaultSensor(BACKTAP_SENSOR_TYPE, true);
                    if (wakeup != null) {
                        setObjectField(wrapper, "mBackTapSensor", wakeup);
                        AndroidLog.d(TAG, "system", "back-tap sensor switched to wakeup variant");
                    } else {
                        AndroidLog.w(TAG, "system", "wakeup back-tap sensor not found");
                    }
                } catch (Throwable t) {
                    AndroidLog.w(TAG, "system", "switch sensor failed", t);
                }
            }
        });

        // Wake the display before shortcut dispatch when screen is off.
        findAndHookMethod("com.miui.server.input.util.ShortCutActionsUtils", "triggerFunction",
            String.class, String.class, android.os.Bundle.class, boolean.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!PrefsBridge.getBoolean("system_framework_back_tap_when_locked")) return;
                String action = (String) param.getArgs()[1];
                if (action == null || (!"back_double_tap".equals(action) && !"back_triple_tap".equals(action))) return;
                Object utils = param.getThisObject();
                try {
                    PowerManager pm = (PowerManager) getObjectField(utils, "mPowerManager");
                    if (pm == null) return;
                    if (!pm.isInteractive()) {
                        callMethod(pm, "wakeUp", android.os.SystemClock.uptimeMillis(), 5, "hyperceiler_back_tap");
                    }
                } catch (Throwable t) {
                    // getPowerManager may not exist on this build; ignore.
                }
            }
        });

        // Keep listener registered on screen-off.
        findAndHookMethod("com.miui.server.input.MiuiBackTapGestureService", "notifyScreenOff", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (PrefsBridge.getBoolean("system_framework_back_tap_when_locked")) {
                    param.setResult(null);
                }
            }
        });

        findAndHookMethod("com.miui.server.input.MiuiBackTapGestureService", "updateBackTapFeatureState", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!PrefsBridge.getBoolean("system_framework_back_tap_when_locked")) return;
                Object service = param.getThisObject();
                if (!getBooleanField(service, "mIsBackTapSensorListenerRegistered")) {
                    setBooleanField(service, "mScreenOn", true);
                }
            }
        });

        // Make app launches show above keyguard.
        findAndHookMethod("com.miui.server.input.util.ShortCutActionsUtils", "launchApp", Intent.class, android.os.Bundle.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!PrefsBridge.getBoolean("system_framework_back_tap_when_locked")) return;
                Object intentObj = param.getArgs()[0];
                if (intentObj instanceof Intent) {
                    ((Intent) intentObj).addFlags(LAUNCH_FLAGS);
                }
            }
        });
    }
}
