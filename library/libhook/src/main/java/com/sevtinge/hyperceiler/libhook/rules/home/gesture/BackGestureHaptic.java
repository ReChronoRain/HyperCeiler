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
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.lang.reflect.Method;
import java.util.Set;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

// https://github.com/HowieHChen/XiaomiHelper/blob/fb2462aa819eed36b6b8875c85f9cb0887eb1ccd/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/miuihome/gesture/BackGestureHaptic.kt
public class BackGestureHaptic extends BaseHook {

    private static final String TIME_OUT_BLOCKER_KEY = "BLOCKER_ID_FOR_HAPTIC_GESTURE_BACK";

    @Override
    public void init() {
        int mode = PrefsBridge.getStringAsInt("home_gesture_back_haptic", 0);
        if (mode == 1) {
            initEnhancedMode();
        } else if (mode == 2) {
            initDisabledMode();
        }
    }

    private void initEnhancedMode() {
        Class<?> hapticFeedbackCompatV2 = findClassIfExists("com.miui.home.common.hapticfeedback.HapticFeedbackCompatV2");
        if (hapticFeedbackCompatV2 == null) {
            return;
        }

        hookNoArgMethods(hapticFeedbackCompatV2, "performGestureReadyBack", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                startTimeOutBlocker();
            }
        });

        hookNoArgMethods(hapticFeedbackCompatV2, "performGestureBackHandUp", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (isTimeOutBlocked()) {
                    param.setResult(null);
                }
            }
        });

        hookLambdaMethods(hapticFeedbackCompatV2, "performGestureReadyBack", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                performGestureHaptic(param.getThisObject(), 0);
                param.setResult(null);
            }
        });

        hookLambdaMethods(hapticFeedbackCompatV2, "performGestureBackHandUp", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                performGestureHaptic(param.getThisObject(), 1);
                param.setResult(null);
            }
        });

        Class<?> gestureStubViewClass = findClassIfExists("com.miui.home.recents.GestureStubView");
        if (gestureStubViewClass != null) {
            for (Method method : gestureStubViewClass.getDeclaredMethods()) {
                if (!"injectBackKeyEvent".equals(method.getName())) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || parameterTypes[0] != boolean.class) {
                    continue;
                }
                method.setAccessible(true);
                hookMethod(method, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.getArgs()[0] = true;
                    }
                });
            }
        }
    }

    private void initDisabledMode() {
        Set<String> classNames = Set.of(
            "com.miui.home.common.hapticfeedback.HapticFeedbackCompatLinear",
            "com.miui.home.common.hapticfeedback.HapticFeedbackCompatNormal",
            "com.miui.home.common.hapticfeedback.HapticFeedbackCompatV2"
        );

        for (String className : classNames) {
            Class<?> hapticClass = findClassIfExists(className);
            if (hapticClass == null) {
                continue;
            }
            IMethodHook disabledHook = new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(null);
                }
            };
            hookNoArgMethods(hapticClass, "performGestureReadyBack", disabledHook);
            hookNoArgMethods(hapticClass, "performGestureBackHandUp", disabledHook);
        }
    }

    private void startTimeOutBlocker() {
        try {
            Class<?> backgroundThreadClass = findClassIfExists("com.miui.home.common.multithread.BackgroundThread");
            Class<?> timeOutBlockerClass = findClassIfExists("com.miui.home.common.utils.TimeOutBlocker");
            if (backgroundThreadClass == null || timeOutBlockerClass == null) {
                return;
            }
            Object handler = callStaticMethod(backgroundThreadClass, "getHandler");
            if (handler != null) {
                callStaticMethod(timeOutBlockerClass, "startCountDown", handler, 140L, TIME_OUT_BLOCKER_KEY);
            }
        } catch (Throwable throwable) {
            XposedLog.w(TAG, getPackageName(), "start back gesture haptic blocker failed", throwable);
        }
    }

    private boolean isTimeOutBlocked() {
        try {
            Class<?> timeOutBlockerClass = findClassIfExists("com.miui.home.common.utils.TimeOutBlocker");
            if (timeOutBlockerClass == null) {
                return false;
            }
            Object blocked = callStaticMethod(timeOutBlockerClass, "isBlocked", TIME_OUT_BLOCKER_KEY);
            return blocked instanceof Boolean && (Boolean) blocked;
        } catch (Throwable throwable) {
            XposedLog.w(TAG, getPackageName(), "check back gesture haptic blocker failed", throwable);
            return false;
        }
    }

    private void performGestureHaptic(Object target, int effectId) {
        Object hapticHelper = getObjectField(target, "mHapticHelper");
        if (hapticHelper == null) {
            return;
        }
        try {
            callMethod(hapticHelper, "performExtHapticFeedback", effectId);
        } catch (Throwable throwable) {
            XposedLog.w(TAG, getPackageName(), "perform back gesture haptic feedback failed", throwable);
        }
    }

    private void hookNoArgMethods(Class<?> targetClass, String methodName, IMethodHook callback) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!methodName.equals(method.getName()) || method.getParameterCount() != 0) {
                continue;
            }
            method.setAccessible(true);
            hookMethod(method, callback);
        }
    }

    private void hookLambdaMethods(Class<?> targetClass, String methodTag, IMethodHook callback) {
        for (Method method : targetClass.getDeclaredMethods()) {
            String methodName = method.getName();
            if (!methodName.startsWith("lambda") || !methodName.contains(methodTag)) {
                continue;
            }
            method.setAccessible(true);
            hookMethod(method, callback);
        }
    }
}
