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

package com.sevtinge.hyperceiler.libhook.rules.various.input;

import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.util.TypedValue;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodBottomManagerHelper;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodConfig;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface;

/**
 * Source:
 * <a href="https://github.com/Howard20181/Mi_AOSP_IME/blob/main/MiAOSPIME/src/main/java/io/github/howard20181/ime/ImeHook.java">Howard20181/Mi_AOSP_IME</a>
 */
public class MiAospIme extends BaseHook {
    private static final WeakHashMap<View, int[]> BASE_PADDINGS = new WeakHashMap<>();

    @Override
    public void init() {
        hookInputMethodService();
        hookNavigationBarController();
        hookNavigationBarInflaterView();
        hookNavigationBarView();
        hookDeadZone();
    }

    public void initLoader(ClassLoader classLoader) {
        hookInputMethodBottomManager(classLoader);
    }

    private void hookInputMethodService() {
        Class<?> inputMethodService = findClassIfExists("android.inputmethodservice.InputMethodService");
        if (inputMethodService == null) return;

        Field internationalBuildField = null;
        try {
            internationalBuildField = inputMethodService.getDeclaredField("IS_INTERNATIONAL_BUILD");
            internationalBuildField.setAccessible(true);
        } catch (Throwable t) {
            XposedLog.w(TAG, "Field IS_INTERNATIONAL_BUILD not found: " + t);
        }

        Field finalInternationalBuildField = internationalBuildField;
        try {
            findAndChainMethod(inputMethodService, "hideImeRenderGesturalNavButtons",
                String.class,
                new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                        if (finalInternationalBuildField != null &&
                            chain.getThisObject() instanceof InputMethodService inputMethodService) {
                            try {
                                Class<?> inputMethodServiceStub = findClassIfExists("android.inputmethodservice.InputMethodServiceStub");
                                if (inputMethodServiceStub != null) {
                                    Object inputMethodServiceInjector = callStaticMethod(inputMethodServiceStub, "getInstance");
                                    if (inputMethodServiceInjector != null) {
                                        Method imeSupportMethod = findImeSupportMethod(inputMethodServiceInjector.getClass());
                                        boolean shouldForceInternationalBuild = true;
                                        if (imeSupportMethod != null) {
                                            imeSupportMethod.setAccessible(true);
                                            Object imeSupport = imeSupportMethod.invoke(
                                                inputMethodServiceInjector,
                                                inputMethodService.getApplicationContext()
                                            );
                                            if (imeSupport instanceof Boolean isImeSupport) {
                                                shouldForceInternationalBuild = !isImeSupport;
                                            }
                                        }
                                        if (shouldForceInternationalBuild) {
                                            finalInternationalBuildField.setBoolean(chain.getThisObject(), true);
                                        }
                                    }
                                }
                            } catch (Throwable t) {
                                XposedLog.w(TAG, "Failed to force IS_INTERNATIONAL_BUILD: " + t);
                            }
                        }
                        return chain.proceed();
                    }
                });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Hook hideImeRenderGesturalNavButtons failed: " + t);
        }
    }

    private void hookNavigationBarController() {
        Class<?> navigationBarController = findClassIfExists("android.inputmethodservice.NavigationBarController$Impl");
        if (navigationBarController == null) return;

        try {
            Field imeDrawsImeNavBarField = navigationBarController.getDeclaredField("mImeDrawsImeNavBar");
            imeDrawsImeNavBarField.setAccessible(true);
            Field serviceField = navigationBarController.getDeclaredField("mService");
            serviceField.setAccessible(true);

            chainAllMethods(navigationBarController, "getImeCaptionBarHeight", new XposedInterface.Hooker() {
                @Override
                public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                    try {
                        boolean showImeNavBar = false;
                        if (!chain.getArgs().isEmpty() && chain.getArg(0) instanceof Boolean enabled) {
                            showImeNavBar = enabled;
                        } else if (chain.getThisObject() != null) {
                            showImeNavBar = imeDrawsImeNavBarField.getBoolean(chain.getThisObject());
                        }

                        if (showImeNavBar && chain.getThisObject() != null) {
                            Object service = serviceField.get(chain.getThisObject());
                            if (service instanceof InputMethodService inputMethodService) {
                                return dpToPx(48, inputMethodService.getResources());
                            }
                        }
                    } catch (Throwable t) {
                        XposedLog.w(TAG, "Hook getImeCaptionBarHeight failed: " + t);
                    }
                    return chain.proceed();
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare NavigationBarController hook failed: " + t);
        }
    }

    private void hookNavigationBarView() {
        Class<?> navigationBarView = findClassIfExists("android.inputmethodservice.navigationbar.NavigationBarView");
        if (navigationBarView == null) return;

        try {
            Field horizontalField = navigationBarView.getDeclaredField("mHorizontal");
            horizontalField.setAccessible(true);

            findAndChainMethod(navigationBarView, "updateOrientationViews", new XposedInterface.Hooker() {
                @Override
                public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    try {
                        if (chain.getThisObject() == null) {
                            return result;
                        }

                        Object horizontalView = horizontalField.get(chain.getThisObject());
                        if (!(horizontalView instanceof View view)) {
                            return result;
                        }

                        int shadow = dpToPx(4, view.getResources());
                        view.setOnApplyWindowInsetsListener((target, insets) ->
                            applyRoundedCornerPadding(target, insets, shadow));
                    } catch (Throwable t) {
                        XposedLog.w(TAG, "Hook updateOrientationViews failed: " + t);
                    }
                    return result;
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare NavigationBarView hook failed: " + t);
        }
    }

    private void hookNavigationBarInflaterView() {
        Class<?> navigationBarInflaterView =
            findClassIfExists("android.inputmethodservice.navigationbar.NavigationBarInflaterView");
        if (navigationBarInflaterView == null) return;

        try {
            findAndChainMethod(navigationBarInflaterView, "inflateLayout",
                String.class,
                new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                        if (!chain.getArgs().isEmpty() && chain.getArg(0) instanceof String) {
                            String navBarLayoutHandle = InputMethodConfig.getAospImeNavBarLayoutHandle();
                            if (!navBarLayoutHandle.isBlank()) {
                                Object[] args = chain.getArgs().toArray();
                                args[0] = navBarLayoutHandle;
                                return chain.proceed(args);
                            }
                        }
                        return chain.proceed();
                    }
                });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare NavigationBarInflaterView hook failed: " + t);
        }
    }

    private void hookDeadZone() {
        Class<?> deadZone = findClassIfExists("android.inputmethodservice.navigationbar.DeadZone");
        if (deadZone == null) return;

        try {
            Field sizeMinField = deadZone.getDeclaredField("mSizeMin");
            sizeMinField.setAccessible(true);

            findAndChainMethod(deadZone, "onConfigurationChanged", int.class, new XposedInterface.Hooker() {
                @Override
                public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    try {
                        if (chain.getThisObject() != null) {
                            sizeMinField.setInt(chain.getThisObject(), 0);
                        }
                    } catch (Throwable t) {
                        XposedLog.w(TAG, "Hook DeadZone.onConfigurationChanged failed: " + t);
                    }
                    return result;
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare DeadZone hook failed: " + t);
        }
    }

    private void hookInputMethodBottomManager(ClassLoader classLoader) {
        try {
            Class<?> getImeClass = InputMethodBottomManagerHelper.findBottomManagerClass(classLoader);
            if (getImeClass == null) {
                throw new ClassNotFoundException("com.miui.inputmethod.InputMethodBottomManager");
            }
            findAndChainMethod(
                getImeClass,
                "getSupportIme",
                new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        try {
                            Object enabledInputMethodList =
                                InputMethodBottomManagerHelper.getEnabledInputMethodList(classLoader);
                            if (enabledInputMethodList != null) {
                                return enabledInputMethodList;
                            }
                        } catch (Throwable t) {
                            XposedLog.w(TAG, "Hook getSupportIme failed: " + t);
                        }
                        return chain.proceed();
                    }
                }
            );
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare InputMethodBottomManager hook failed: " + t);
        }
    }

    private Method findImeSupportMethod(Class<?> clazz) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredMethod("isImeSupport", Context.class);
            } catch (NoSuchMethodException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    private WindowInsets applyRoundedCornerPadding(View view, WindowInsets insets, int shadow) {
        int[] basePadding = BASE_PADDINGS.computeIfAbsent(view, target -> new int[]{
            target.getPaddingLeft() + shadow,
            target.getPaddingTop(),
            target.getPaddingRight() + shadow,
            target.getPaddingBottom()
        });

        int leftRadius = 0;
        int rightRadius = 0;
        try {
            RoundedCorner bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT);
            RoundedCorner bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT);
            leftRadius = bottomLeft != null ? bottomLeft.getRadius() : 0;
            rightRadius = bottomRight != null ? bottomRight.getRadius() : 0;
        } catch (Throwable t) {
            XposedLog.w(TAG, "Read rounded corner failed: " + t);
        }

        view.setPadding(
            leftRadius > 0 ? Math.max(basePadding[0], leftRadius - basePadding[0]) : basePadding[0],
            basePadding[1],
            rightRadius > 0 ? Math.max(basePadding[2], rightRadius - basePadding[2]) : basePadding[2],
            basePadding[3]
        );
        return insets;
    }

    private int dpToPx(int data, Resources resources) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, data, resources.getDisplayMetrics()));
    }
}
