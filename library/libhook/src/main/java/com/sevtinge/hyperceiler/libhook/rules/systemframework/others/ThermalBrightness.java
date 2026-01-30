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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class ThermalBrightness extends BaseHook {
    public final String displayPowerControllerImpl = "com.android.server.display.DisplayPowerControllerImpl";
    public final String automaticBrightnessControllerImpl = "com.android.server.display.AutomaticBrightnessControllerImpl";
    public final String thermalBrightnessController = "com.android.server.display.ThermalBrightnessController";
    public final String temperatureController = "com.android.server.display.TemperatureController";
    public final String thermalHelper = "com.android.server.audio.MultimediaMiPerception.utils.ThermalHelper";
    public final String thermalObserver = "com.android.server.display.ThermalObserver";


    @Override
    public void init() {
        try {
            findClass(displayPowerControllerImpl);
            findAndHookConstructor(displayPowerControllerImpl,
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        setBooleanField(param, "SUPPORT_TEMEPERATURE_CONTROL", false);
                        setBooleanField(param, "mThermalBrightnessControlAvailable", false);
                        setBooleanField(param, "mApplyThermalBrightnessRate", false);
                    }
                }
            );
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "No found class: " + e);
        }

        try {
            findClass(automaticBrightnessControllerImpl);
            findAndHookConstructor(automaticBrightnessControllerImpl,
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        setBooleanField(param, "SUPPORT_TEMEPERATURE_CONTROL", false);
                    }
                }
            );
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "No found class: " + e);
        }

        if (isMoreAndroidVersion(36)) {
            try {
                findClass(thermalHelper).getDeclaredMethod("updateTemperature");
                findAndHookMethod(thermalHelper,
                    "updateTemperature", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.setResult(null);
                        }
                    }
                );
            } catch (NoSuchMethodException e) {
                XposedLog.e(TAG, getPackageName(), "Don't Have updateTemperature: " + e);
            } catch (Throwable ne) {
                XposedLog.e(TAG, getPackageName(), "Class not found: " + ne);
            }
        } else {
            try {
                findClass(temperatureController).getDeclaredMethod("updateTemperature");
                findAndHookMethod(temperatureController,
                    "updateTemperature", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.setResult(null);
                        }
                    }
                );
            } catch (NoSuchMethodException e) {
                XposedLog.e(TAG, getPackageName(), "Don't Have updateTemperature: " + e);
            } catch (Throwable ne) {
                XposedLog.e(TAG, getPackageName(), "Class not found: " + ne);
            }
        }

        try {
            findClass(thermalObserver).getDeclaredMethod("updateTemperature");
            findAndHookMethod(thermalObserver,
                "updateTemperature", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            XposedLog.e(TAG, getPackageName(), "Don't Have updateTemperature: " + e);
        } catch (Throwable ne) {
            XposedLog.e(TAG, getPackageName(), "Class not found: " + ne);
        }

        hookAllMethods("com.android.server.display.DisplayPowerControllerImpl",
            "adjustBrightnessByThermal", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(param.getArgs()[0]);
                }
            }
        );

        try {
            findClassIfExists(displayPowerControllerImpl).getDeclaredMethod("updateThermalBrightness", float.class);
            findAndHookMethod(displayPowerControllerImpl,
                "updateThermalBrightness", float.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            XposedLog.e(TAG, getPackageName(), "Don't Have updateThermalBrightness: " + e);
        }

        try {
            findClassIfExists(thermalBrightnessController).getDeclaredMethod("updateThermalBrightnessIfNeeded");
            findAndHookMethod(thermalBrightnessController,
                "updateThermalBrightnessIfNeeded", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.setResult(false);
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            XposedLog.e(TAG, getPackageName(), "Don't Have updateThermalBrightnessIfNeeded: " + e);
        }

        try {
            findClassIfExists(thermalBrightnessController).getDeclaredMethod("updateConditionState", int.class);
            findAndHookMethod(thermalBrightnessController, "updateConditionState", int.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(null);
                }
            });
        } catch (NoSuchMethodException e) {
            XposedLog.e(TAG, getPackageName(), "Don't Have updateConditionState: " + e);
        }
    }
}
