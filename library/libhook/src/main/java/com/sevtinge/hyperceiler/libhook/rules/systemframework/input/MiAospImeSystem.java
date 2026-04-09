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

package com.sevtinge.hyperceiler.libhook.rules.systemframework.input;

import android.content.Context;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodConfig;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;

/**
 * Source:
 * <a href="https://github.com/Howard20181/Mi_AOSP_IME/blob/main/MiAOSPIME/src/main/java/io/github/howard20181/ime/ImeHook.java">Howard20181/Mi_AOSP_IME</a>
 */
public class MiAospImeSystem extends BaseHook {
    private static final int INPUT_METHOD_NAV_BUTTON_FLAG_IME_DRAWS_IME_NAV_BAR = 1;

    @Override
    public void init() {
        hookInputMethodManagerService();
        hookCustomizedInputMethod();
        hookInputMethodManagerServiceImpl();
    }

    private void hookInputMethodManagerService() {
        Class<?> inputMethodManagerService = findClassIfExists("com.android.server.inputmethod.InputMethodManagerService");
        if (inputMethodManagerService == null) {
            return;
        }

        Field contextField = null;
        try {
            contextField = inputMethodManagerService.getDeclaredField("mContext");
            contextField.setAccessible(true);
        } catch (Throwable t) {
            XposedLog.w(TAG, "Prepare InputMethodManagerService context hook failed: " + t);
        }

        final Field finalContextField = contextField;

        try {
            chainAllMethods(inputMethodManagerService, "getInputMethodNavButtonFlagsLocked", new XposedInterface.Hooker() {
                @Override
                public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    try {
                        if (!(result instanceof Integer navButtonFlags)) {
                            return result;
                        }

                        Context systemContext = getSystemContext(chain.getThisObject(), finalContextField);
                        if (systemContext == null) {
                            return result;
                        }

                        boolean originalEnabled =
                            (navButtonFlags & INPUT_METHOD_NAV_BUTTON_FLAG_IME_DRAWS_IME_NAV_BAR) != 0;
                        boolean fallbackEnabled = shouldEnableAospImeNavBarFallback(systemContext);
                        if (fallbackEnabled == originalEnabled) {
                            return result;
                        }

                        updateImeNavBarFlag(chain.getArgs().toArray(), fallbackEnabled);
                        return fallbackEnabled
                            ? navButtonFlags | INPUT_METHOD_NAV_BUTTON_FLAG_IME_DRAWS_IME_NAV_BAR
                            : navButtonFlags & ~INPUT_METHOD_NAV_BUTTON_FLAG_IME_DRAWS_IME_NAV_BAR;
                    } catch (Throwable t) {
                        XposedLog.w(TAG, "Update getInputMethodNavButtonFlagsLocked result failed: " + t);
                    }
                    return result;
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare InputMethodManagerService hook failed: " + t);
        }
    }

    private void hookInputMethodManagerServiceImpl() {
        Class<?> inputMethodManagerServiceImpl = findClassIfExists("com.android.server.inputmethod.InputMethodManagerServiceImpl");
        if (inputMethodManagerServiceImpl == null) {
            return;
        }

        try {
            findAndChainMethod(inputMethodManagerServiceImpl, "isCallingBetweenCustomIME",
                Context.class, int.class, String.class,
                new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(result) || chain.getArgs().size() < 2) {
                            return result;
                        }

                        if (!(chain.getArg(0) instanceof Context context) ||
                            !(chain.getArg(1) instanceof Integer uid)) {
                            return result;
                        }

                        try {
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm == null) {
                                return result;
                            }

                            InputMethodInfo inputMethodInfo = imm.getCurrentInputMethodInfo();
                            if (inputMethodInfo == null) {
                                return result;
                            }

                            String currentImePackage = inputMethodInfo.getPackageName();
                            if (!InputMethodConfig.isAospImePackage(currentImePackage)) {
                                return result;
                            }

                            String[] packages = context.getPackageManager().getPackagesForUid(uid);
                            if (packages == null) {
                                return result;
                            }

                            for (String packageName : packages) {
                                if (currentImePackage.equals(packageName)) {
                                    return true;
                                }
                            }
                        } catch (Throwable t) {
                            XposedLog.w(TAG, "Hook isCallingBetweenCustomIME failed: " + t);
                        }
                        return result;
                    }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare InputMethodManagerServiceImpl hook failed: " + t);
        }
    }

    private void hookCustomizedInputMethod() {
        Class<?> customizedInputMethodClass =
            findClassIfExists("com.android.server.inputmethod.InputMethodManagerServiceImpl");
        if (customizedInputMethodClass == null) {
            return;
        }

        try {
            chainAllMethods(customizedInputMethodClass, "isCustomizedInputMethod", new XposedInterface.Hooker() {
                @Override
                public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    if (!chain.getArgs().isEmpty() && chain.getArg(0) instanceof String inputMethodId) {
                        String selectedPackage = InputMethodConfig.normalizePackageName(inputMethodId);
                        if (InputMethodConfig.isAospImePackage(selectedPackage)) {
                            return false;
                        }
                    }
                    return result;
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "Prepare InputMethodManagerServiceImpl isCustomizedInputMethod hook failed: " + t);
        }
    }

    private Context getSystemContext(Object service, Field contextField) {
        if (service == null || contextField == null) {
            return null;
        }

        try {
            Object context = contextField.get(service);
            return context instanceof Context ? (Context) context : null;
        } catch (Throwable t) {
            XposedLog.w(TAG, "Read system context failed: " + t);
            return null;
        }
    }

    private boolean shouldEnableAospImeNavBarFallback(Context context) {
        return isGestureNavigationEnabled(context) &&
            InputMethodConfig.isAospImePackage(
                InputMethodConfig.normalizePackageName(getSelectedInputMethodId(context)));
    }

    private boolean isGestureNavigationEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "navigation_mode", 2) == 2;
    }

    private String getSelectedInputMethodId(Context context) {
        return Settings.Secure.getString(
            context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
    }

    private void updateImeNavBarFlag(Object[] args, boolean enabled) {
        if (args == null || args.length == 0 || args[0] == null) {
            return;
        }

        updateUserDataImeNavBarFlag(args[0], enabled);
    }

    private void updateUserDataImeNavBarFlag(Object userData, boolean enabled) {
        try {
            Field imeDrawsNavBarField = userData.getClass().getDeclaredField("mImeDrawsNavBar");
            imeDrawsNavBarField.setAccessible(true);
            Object imeDrawsNavBar = imeDrawsNavBarField.get(userData);
            if (imeDrawsNavBar instanceof AtomicBoolean atomicBoolean) {
                atomicBoolean.set(enabled);
            }
        } catch (NoSuchFieldException ignored) {
        } catch (Throwable t) {
            XposedLog.w(TAG, "Update UserData.mImeDrawsNavBar failed: " + t);
        }
    }
}
