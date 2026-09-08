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

import android.view.View;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;
import io.github.libxposed.api.XposedInterface;

// thanks: https://github.com/HowieHChen/XiaomiHelper/blob/67c3459acd1b9b27b230f991a0cd72bfffd5aa92/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/miuihome/gesture/PredictiveBackProgress.kt
public class PredictiveBackProgress extends HomeBaseHookNew {

    private static final String GESTURE_STUB_VIEW = "com.miui.home.recents.GestureStubView";
    private static final float PROGRESS_THRESHOLD_DP = 412.0f;
    private static final float FALLBACK_THRESHOLD_PX = 180.0f;

    private final Object mNewHomeHookLock = new Object();
    private volatile boolean mSwipeProcessHooked = false;
    private volatile Method mBackMotionEventFactory = null;

    @Override
    public void initBase() {
        Class<?> gestureStubViewClass = findClassIfExists(GESTURE_STUB_VIEW);
        if (gestureStubViewClass == null || !hasOldBackProgressMethod(gestureStubViewClass)) {
            return;
        }
        findAndHookMethod(
            gestureStubViewClass,
            "onBackProgressed",
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            int.class,
            Object.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.getArgs()[2] = computeProgress(param.getThisObject());
                }
            }
        );
    }

    @Version(min = 600000000)
    private void initNewHomeHook() {
        Class<?> gestureStubViewClass = findClassIfExists(GESTURE_STUB_VIEW);
        if (gestureStubViewClass == null) {
            return;
        }
        hookAllConstructors(gestureStubViewClass, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                ensureSwipeProcessHook(getObjectField(param.getThisObject(), "mGesturesBackCallback"));
            }
        });
    }

    private boolean hasOldBackProgressMethod(Class<?> gestureStubViewClass) {
        try {
            gestureStubViewClass.getDeclaredMethod(
                "onBackProgressed",
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                int.class,
                Object.class
            );
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private void ensureSwipeProcessHook(Object gesturesBackCallback) {
        if (mSwipeProcessHooked || gesturesBackCallback == null) {
            return;
        }
        synchronized (mNewHomeHookLock) {
            if (mSwipeProcessHooked) {
                return;
            }
            Class<?> callbackClass = gesturesBackCallback.getClass();
            Method swipeProcessMethod = findSwipeProcessMethod(callbackClass);
            if (swipeProcessMethod == null) {
                return;
            }
            XposedInterface.HookHandle handle = findAndHookMethod(callbackClass, "onSwipeProcess", float.class, new IMethodHook() {
                @Override
                public void before(HookParam param) throws Throwable {
                    Object gestureStubView = getObjectField(param.getThisObject(), "this$0");
                    if (!(gestureStubView instanceof View)) {
                        return;
                    }
                    Object controller = getObjectField(gestureStubView, "mOnBackInvokedCallbackController");
                    Object backMotionEvent = newBackMotionEvent(gestureStubView, computeProgress(gestureStubView));
                    if (controller == null || backMotionEvent == null) {
                        return;
                    }
                    Object arrowView = getObjectField(gestureStubView, "mGestureBackArrowView");
                    if (arrowView != null) {
                        callMethod(arrowView, "onSwipeProgress", param.getArgs()[0]);
                    }
                    callMethod(controller, "onBackProgressed", backMotionEvent);
                    param.setResult(null);
                }
            });
            if (handle != null) {
                mSwipeProcessHooked = true;
            }
        }
    }

    private Method findSwipeProcessMethod(Class<?> callbackClass) {
        for (Method method : callbackClass.getDeclaredMethods()) {
            if (!"onSwipeProcess".equals(method.getName())) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0] == float.class) {
                return method;
            }
        }
        return null;
    }

    private Object newBackMotionEvent(Object gestureStubView, float progress) {
        try {
            Class<?> providerClass = findClassIfExists("android.window.BackMotionEventProvider");
            if (providerClass == null) {
                return null;
            }
            Method factory = getBackMotionEventFactory(providerClass);
            if (factory == null) {
                return null;
            }
            return factory.invoke(
                null,
                getFloatField(gestureStubView, "mCurrX"),
                getFloatField(gestureStubView, "mCurrY"),
                progress,
                0.0f,
                0.0f,
                getIntField(gestureStubView, "mGestureStubPos") == 0 ? 0 : 1,
                null
            );
        } catch (Throwable throwable) {
            XposedLog.w(TAG, getPackageName(), "build BackMotionEvent failed", throwable);
            return null;
        }
    }

    private Method getBackMotionEventFactory(Class<?> providerClass) {
        Method cachedFactory = mBackMotionEventFactory;
        if (cachedFactory != null) {
            return cachedFactory;
        }
        for (Method method : providerClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!"getInstance".equals(method.getName()) || method.getParameterCount() != 7) {
                continue;
            }
            method.setAccessible(true);
            mBackMotionEventFactory = method;
            return method;
        }
        return null;
    }

    private float computeProgress(Object gestureStubView) {
        float distance = Math.abs(getFloatField(gestureStubView, "mCurrX") - getFloatField(gestureStubView, "mDownX"));
        float threshold = resolveFullyStretchedThreshold(gestureStubView);
        return clamp(distance / threshold);
    }

    private float resolveFullyStretchedThreshold(Object gestureStubView) {
        float density = getFloatField(gestureStubView, "mDensity");
        int screenWidth = getIntField(gestureStubView, "mScreenWidth");
        if (gestureStubView instanceof View view) {
            if (density <= 0.0f) {
                density = view.getResources().getDisplayMetrics().density;
            }
            if (screenWidth <= 0) {
                screenWidth = view.getResources().getDisplayMetrics().widthPixels;
            }
        }
        float progressThreshold = density > 0.0f ? density * PROGRESS_THRESHOLD_DP : FALLBACK_THRESHOLD_PX;
        if (screenWidth > 0) {
            progressThreshold = Math.min(screenWidth, progressThreshold);
        }
        return progressThreshold > 0.0f ? progressThreshold : FALLBACK_THRESHOLD_PX;
    }

    private float clamp(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        return Math.min(value, 1.0f);
    }

    private float getFloatField(Object target, String fieldName) {
        Object value = getObjectField(target, fieldName);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0f;
    }

    private int getIntField(Object target, String fieldName) {
        Object value = getObjectField(target, fieldName);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
