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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.OpCodeMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;
import java.util.List;

public class UnlockCarSicknessRelief extends BaseHook {
    private static final String SETTINGS_FRAGMENT =
        "com.miui.carsickness.ui.CarSicknessReliefSettingsActivity$MotionSicknessReliefSettingsFragment";
    private static final String REMIND_TIME_SETTING = "motion_sickness_relief_remind_time";
    private static final int DRIVING_SENSOR_TYPE = 33171091;
    private static final int REMIND_WHILE_DRIVING = 0;
    private static final int REMIND_ALWAYS = 1;

    private Method mUnlockCarSicknessReliefMethod;
    private Method mUnlockCarSicknessRemindTimingMethod;
    private Method mGetRemindTimingMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        if (isMoreHyperOSVersion(4f)) {
            initDexKitOS4();
            return true;
        }

        mUnlockCarSicknessReliefMethod = requiredMember("UnlockCarSicknessRelief", bridge ->
            bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .usingStrings("is_support_carsick")
                .returnType(boolean.class)
                .paramTypes(Context.class))).singleOrNull());
        mUnlockCarSicknessRemindTimingMethod = requiredMember("UnlockCarSicknessRemindTiming", bridge ->
            bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .addCaller(MethodMatcher.create().declaredClass(SETTINGS_FRAGMENT).name("onCreatePreferences"))
                .returnType(boolean.class)
                .paramTypes(Context.class))).singleOrNull());
        return true;
    }

    private void initDexKitOS4() {
        mUnlockCarSicknessReliefMethod = requiredMember("UnlockCarSicknessReliefOS4", bridge ->
            bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .declaredClass(ClassMatcher.create().usingStrings("CarSicknessUtils", REMIND_TIME_SETTING))
                .addInvoke("Landroid/hardware/SensorManager;->getDefaultSensor(I)Landroid/hardware/Sensor;")
                .usingNumbers(Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_GYROSCOPE)
                .returnType(boolean.class)
                .paramTypes(Context.class))).singleOrNull());

        Class<?> utilsClass = mUnlockCarSicknessReliefMethod.getDeclaringClass();
        mUnlockCarSicknessRemindTimingMethod = optionalMember("UnlockCarSicknessRemindTimingOS4", bridge ->
            bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .declaredClass(utilsClass)
                .addCaller(MethodMatcher.create()
                    .declaredClass(SETTINGS_FRAGMENT)
                    .name("onCreatePreferences")
                    .usingStrings("remind_time_setting"))
                .returnType(boolean.class)
                .paramTypes(Context.class))).singleOrNull());
        mGetRemindTimingMethod = optionalMember("GetCarSicknessRemindTimingOS4", bridge ->
            bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .declaredClass(utilsClass)
                .addCaller(MethodMatcher.create()
                    .declaredClass(SETTINGS_FRAGMENT)
                    .name("onPreferenceChange")
                    .usingStrings("miui.carsickness.remind_always", "miui.carsickness.remind_driving"))
                .addCaller(MethodMatcher.create()
                    .declaredClass("com.miui.carsickness.service.CarSicknessService"))
                .opNames(List.of("const/4", "return"), OpCodeMatchType.Equals)
                .usingNumbers(REMIND_ALWAYS)
                .returnType(int.class)
                .paramTypes(Context.class))).singleOrNull());
    }

    @Override
    public void init() {
        if (!isMoreHyperOSVersion(4f)) {
            hookMethod(mUnlockCarSicknessReliefMethod, returnConstant(true));
            hookMethod(mUnlockCarSicknessRemindTimingMethod, returnConstant(true));
            return;
        }

        chain(mUnlockCarSicknessReliefMethod, invocation -> {
            if (Boolean.TRUE.equals(invocation.proceed())) return true;
            Context context = (Context) invocation.getArg(0);
            SensorManager sensors = context.getSystemService(SensorManager.class);
            return sensors != null
                && sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
                && sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
        });
        if (mUnlockCarSicknessRemindTimingMethod == null || mGetRemindTimingMethod == null) return;

        // This security center version writes the setting but its reader always returns REMIND_ALWAYS.
        var timingReader = chain(mGetRemindTimingMethod, invocation -> {
            Context context = (Context) invocation.getArg(0);
            if (!supportsDrivingReminder(context)) return invocation.proceed();
            int timing = Settings.System.getInt(context.getContentResolver(), REMIND_TIME_SETTING, REMIND_ALWAYS);
            return timing == REMIND_WHILE_DRIVING ? REMIND_WHILE_DRIVING : REMIND_ALWAYS;
        });
        if (timingReader != null) {
            chain(mUnlockCarSicknessRemindTimingMethod, invocation ->
                supportsDrivingReminder((Context) invocation.getArg(0)) || Boolean.TRUE.equals(invocation.proceed()));
        }
    }

    private static boolean supportsDrivingReminder(Context context) {
        SensorManager sensors = context.getSystemService(SensorManager.class);
        return sensors != null && sensors.getDefaultSensor(DRIVING_SENSOR_TYPE, true) != null;
    }
}

