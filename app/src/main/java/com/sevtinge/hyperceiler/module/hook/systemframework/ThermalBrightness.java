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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class ThermalBrightness extends BaseHook {
    public final String displayPowerControllerImpl = "com.android.server.display.DisplayPowerControllerImpl";
    public final String automaticBrightnessControllerImpl = "com.android.server.display.AutomaticBrightnessControllerImpl";
    public final String thermalBrightnessController = "com.android.server.display.ThermalBrightnessController";
    public final String temperatureController = "com.android.server.display.TemperatureController";

    @Override
    public void init() {
        try {
            XposedHelpers.findClass(displayPowerControllerImpl, lpparam.classLoader);
            findAndHookConstructor(displayPowerControllerImpl,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        setDeclaredField(param, "SUPPORT_TEMEPERATURE_CONTROL", false);
                        setDeclaredField(param, "mThermalBrightnessControlAvailable", false);
                        setDeclaredField(param, "mApplyThermalBrightnessRate", false);
                    }
                }
            );
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(TAG, "android", "No found class: " + e);
        }

        try {
            XposedHelpers.findClass(automaticBrightnessControllerImpl, lpparam.classLoader);
            findAndHookConstructor(automaticBrightnessControllerImpl,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        setDeclaredField(param, "SUPPORT_TEMEPERATURE_CONTROL", false);
                    }
                }
            );
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(TAG, "android", "No found class: " + e);
        }

        try {
            findClassIfExists(temperatureController).getDeclaredMethod("updateTemperature");
            findAndHookMethod(temperatureController,
                "updateTemperature", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            logE(TAG, "android", "Don't Have updateTemperature: " + e);
        }

        /*hookAllMethods("com.android.server.display.DisplayPowerControllerImpl",
            "adjustBrightnessByThermal", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(param.args[0]);
                }
            }
        );*/

        try {
            findClassIfExists(displayPowerControllerImpl).getDeclaredMethod("updateThermalBrightness", float.class);
            findAndHookMethod(displayPowerControllerImpl,
                "updateThermalBrightness", float.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            logE(TAG, "android", "Don't Have updateThermalBrightness: " + e);
        }

        try {
            findClassIfExists(thermalBrightnessController).getDeclaredMethod("updateThermalBrightnessIfNeeded");
            findAndHookMethod(thermalBrightnessController,
                "updateThermalBrightnessIfNeeded", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(false);
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            logE(TAG, "android", "Don't Have updateThermalBrightnessIfNeeded: " + e);
        }
    }
}
