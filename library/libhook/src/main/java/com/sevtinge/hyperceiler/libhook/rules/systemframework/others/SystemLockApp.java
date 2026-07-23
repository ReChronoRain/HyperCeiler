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

import static com.sevtinge.hyperceiler.libhook.base.BaseHook.setBooleanField;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.WindowInsets;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.libxposed.api.XposedInterface;

/**
 * @author 焕晨HChen
 */
public class SystemLockApp extends BaseHook {
    private static final long POWER_HOLD_EXIT_MS = 1000L;
    private static final long SUPPRESS_POWER_MENU_AFTER_EXIT_MS = 1500L;
    private static final long TRANSIENT_BLOCK_LOG_INTERVAL_MS = 1200L;

    private static final String SETTING_KEY_LOCK_APP = "key_lock_app";
    private static final String SETTING_HIDE_GESTURE_LINE = "hide_gesture_line";
    private static final int HIDE_GESTURE_LINE_SHOW = 0;
    private static final int HIDE_GESTURE_LINE_HIDE = 1;

    private static final String MIUI_POWER_KEY_RULE_CLASS = "com.android.server.input.shortcut.singlekeyrule.PowerKeyRule";
    private static final String MIUI_POLICY_CLASS = "com.android.server.policy.BaseMiuiPhoneWindowManager";
    private static final String PHONE_WINDOW_MANAGER_CLASS = "com.android.server.policy.PhoneWindowManager";
    private static final String MIUI_SHORTCUT_ACTIONS_CLASS = "com.miui.server.input.util.ShortCutActionsUtils";
    private static final String INSETS_POLICY_CLASS = "com.android.server.wm.InsetsPolicy";

    private static final String ACTION_LONG_PRESS_POWER_KEY = "long_press_power_key";
    private static final String ACTION_IMPERCEPTIBLE_PRESS_POWER_KEY = "imperceptible_press_power_key";

    private Integer mGestureLineBeforeLock = null;
    boolean isLock = false;
    private volatile long mLastPowerExitUptimeMs = 0L;
    private volatile long mLastTransientBlockLogUptimeMs = 0L;
    private boolean mObserverRegistered = false;

    @Override
    public void init() {
        chainAllMethods("com.android.server.wm.ActivityTaskManagerService",
            "startSystemLockTaskMode",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    long identity = Binder.clearCallingIdentity();
                    try {
                        Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                        if (context == null) return result;

                        int lockTaskId = -1;
                        if (!chain.getArgs().isEmpty() && chain.getArg(0) instanceof Integer) {
                            lockTaskId = (int) chain.getArg(0);
                        }

                        isLock = lockTaskId != -1;
                        mLastPowerExitUptimeMs = 0L;
                        setLockApp(context, lockTaskId);
                        applyGestureLineShielding(context, true);
                        registerObserverIfNeeded(context);
                    } catch (Throwable e) {
                        XposedLog.e(TAG, "startSystemLockTaskMode E: " + e);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                    return result;
                }
            }
        );

        chainAllMethods("com.android.server.wm.ActivityTaskManagerService",
            "stopSystemLockTaskMode",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                    int callingPid = Binder.getCallingPid();

                    // 核心拦截逻辑：只有在锁定状态，且调用者不是系统进程自身，且没有电源键退出标志时才拦截
                    if (isLock && context != null && getLockApp(context) != -1) {
                        if (callingPid != android.os.Process.myPid() && !shouldSuppressPowerActionNow()) {
                            XposedLog.d(TAG, "GuidedAccess: Blocked unauthorized stopSystemLockTaskMode from PID: " + callingPid);
                            return null;
                        }
                    }

                    Object result = chain.proceed();
                    long identity = Binder.clearCallingIdentity();
                    try {
                        if (context == null) return result;

                        isLock = false;
                        setLockApp(context, -1);
                        applyGestureLineShielding(context, false);
                    } catch (Throwable e) {
                        XposedLog.e(TAG, "stopSystemLockTaskMode E: " + e);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                    return result;
                }
            }
        );

        // 确保 key_lock_app 和屏蔽状态一定能恢复。
        chainAllMethods("com.android.server.wm.ActivityTaskManagerService",
            "stopLockTaskModeInternal",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    long identity = Binder.clearCallingIdentity();
                    try {
                        Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                        if (context == null) return result;
                        if (!isLock && getLockApp(context) == -1) return result;

                        isLock = false;
                        setLockApp(context, -1);
                        applyGestureLineShielding(context, false);
                    } catch (Throwable e) {
                        XposedLog.e(TAG, "stopLockTaskModeInternal E: " + e);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                    return result;
                }
            }
        );

        hookMiuiPowerLongPressTimeout();
        hookMiuiPowerPostDelay();
        hookMiuiPowerShortcutTrigger();
        hookSuppressPowerMenuAfterExit();
        hookTransientBarsSuppression();

        findAndChainMethod("com.android.server.wm.LockTaskController",
            "shouldLockKeyguard",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) {
                    return PrefsBridge.getBoolean("system_framework_guided_access_exit_lockscreen", false);
                }
            },
            int.class
        );
    }

    private void registerObserverIfNeeded(Context context) {
        if (mObserverRegistered || context == null) return;
        context.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTING_KEY_LOCK_APP),
            false,
            new ContentObserver(new Handler(context.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (getLockApp(context) == -1 && isLock) {
                        XposedLog.d(TAG, "External unlock detected, calling stopSystemLockTaskMode");
                        stopSystemLockTaskMode();
                    }
                }
            }
        );
        mObserverRegistered = true;
    }

    private void hookMiuiPowerLongPressTimeout() {
        Class<?> ruleClass = findClassIfExists(MIUI_POWER_KEY_RULE_CLASS);
        if (ruleClass != null) {
            try {
                chainAllMethods(ruleClass, "getMiuiLongPressTimeoutMs", new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        if (!isLock) return chain.proceed();
                        return POWER_HOLD_EXIT_MS;
                    }
                });
            } catch (Throwable e) {
                XposedLog.w(TAG, "Hook getMiuiLongPressTimeoutMs failed: " + e);
            }
        }
    }

    private void hookMiuiPowerPostDelay() {
        Class<?> policyClass = findClassIfExists(MIUI_POLICY_CLASS);
        if (policyClass == null) {
            XposedLog.w(TAG, "Miui policy class not found for postKeyFunction: " + MIUI_POLICY_CLASS);
            return;
        }

        try {
            findAndChainMethod(policyClass, "postKeyFunction", new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object[] args = chain.getArgs().toArray();
                    String shortcut = (String) args[2];
                    if (!ACTION_IMPERCEPTIBLE_PRESS_POWER_KEY.equals(shortcut)
                        && !ACTION_LONG_PRESS_POWER_KEY.equals(shortcut)) {
                        return chain.proceed(args);
                    }
                    if (shouldSuppressPowerActionNow()) {
                        return null;
                    }
                    if (!isLock) return chain.proceed(args);

                    args[1] = (int) POWER_HOLD_EXIT_MS;
                    if (ACTION_IMPERCEPTIBLE_PRESS_POWER_KEY.equals(shortcut)) {
                        args[2] = ACTION_LONG_PRESS_POWER_KEY;
                    }
                    return chain.proceed(args);
                }
            }, String.class, int.class, String.class);
        } catch (Throwable e) {
            XposedLog.w(TAG, "Hook postKeyFunction failed: " + e);
        }
    }

    private void hookMiuiPowerShortcutTrigger() {
        XposedInterface.Hooker triggerHooker = new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                String action = (String) chain.getArg(1);
                if (!isLock && !shouldSuppressPowerActionNow()) return chain.proceed();
                return interceptPowerShortcutAction(action, isLock, chain);
            }
        };

        try {
            findAndChainMethod(MIUI_SHORTCUT_ACTIONS_CLASS, "triggerFunction", triggerHooker,
                String.class, String.class, Bundle.class, boolean.class, String.class);
        } catch (Throwable ignored) {}

        try {
            findAndChainMethod(MIUI_SHORTCUT_ACTIONS_CLASS, "triggerFunction", triggerHooker,
                String.class, String.class, Bundle.class, boolean.class);
        } catch (Throwable ignored) {}
    }

    private Object interceptPowerShortcutAction(String action, boolean lockedNow, XposedInterface.Chain chain) throws Throwable {
        if (ACTION_IMPERCEPTIBLE_PRESS_POWER_KEY.equals(action)) return true;
        if (!ACTION_LONG_PRESS_POWER_KEY.equals(action)) return chain.proceed();

        if (lockedNow) {
            XposedLog.d(TAG, "Power long press detected, exiting GuidedAccess");
            markPowerExitAndSuppressMenuWindow();
            stopSystemLockTaskMode();
            return true;
        }

        if (shouldSuppressPowerActionNow()) return true;
        return chain.proceed();
    }

    private void hookSuppressPowerMenuAfterExit() {
        Class<?> pwmClass = findClassIfExists(PHONE_WINDOW_MANAGER_CLASS);
        if (pwmClass == null) return;

        XposedInterface.Hooker suppressHooker = new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                if (isLock) {
                    markPowerExitAndSuppressMenuWindow();
                    stopSystemLockTaskMode();
                    trySetPowerKeyHandled(chain.getThisObject(), true);
                    return null;
                }
                if (!shouldSuppressPowerActionNow()) return chain.proceed();
                trySetPowerKeyHandled(chain.getThisObject(), true);
                return null;
            }
        };

        try {
            chainAllMethods(pwmClass, "powerLongPress", suppressHooker);
            chainAllMethods(pwmClass, "showGlobalActions", suppressHooker);
            chainAllMethods(pwmClass, "showGlobalActionsInternal", suppressHooker);
        } catch (Throwable e) {
            XposedLog.w(TAG, "Hook PWM power actions failed: " + e);
        }
    }

    private void hookTransientBarsSuppression() {
        try {
            chainAllMethods(INSETS_POLICY_CLASS, "showTransient", new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    if (!isLock || !isEnhancedShieldEnabled()) return chain.proceed();
                    int types = (Integer) chain.getArg(0);
                    if ((types & WindowInsets.Type.navigationBars()) == 0) return chain.proceed();
                    debugTransientBlock("block InsetsPolicy.showTransient types=" + types);
                    return null;
                }
            });
        } catch (Throwable ignored) {}
    }

    private void trySetPowerKeyHandled(Object pwmObj, boolean handled) {
        if (pwmObj == null) return;
        try {
            setBooleanField(pwmObj, "mPowerKeyHandled", handled);
        } catch (Throwable ignored) {
        }
    }

    private void markPowerExitAndSuppressMenuWindow() {
        mLastPowerExitUptimeMs = SystemClock.uptimeMillis();
    }

    private void debugTransientBlock(String message) {
        long now = SystemClock.uptimeMillis();
        if (now - mLastTransientBlockLogUptimeMs < TRANSIENT_BLOCK_LOG_INTERVAL_MS) return;
        mLastTransientBlockLogUptimeMs = now;
        XposedLog.d(TAG, message);
    }

    private boolean shouldSuppressPowerActionNow() {
        long mark = mLastPowerExitUptimeMs;
        if (mark <= 0) return false;
        long now = SystemClock.uptimeMillis();
        if (now - mark > SUPPRESS_POWER_MENU_AFTER_EXIT_MS) {
            mLastPowerExitUptimeMs = 0L;
            return false;
        }
        return true;
    }

    private void applyGestureLineShielding(Context context, boolean enteringLockTask) {
        if (context == null) return;

        if (!enteringLockTask) {
            restoreGestureLineState(context);
            return;
        }
        if (!isEnhancedShieldEnabled()) return;

        try {
            if (mGestureLineBeforeLock == null) {
                mGestureLineBeforeLock = Settings.Global.getInt(context.getContentResolver(), SETTING_HIDE_GESTURE_LINE, HIDE_GESTURE_LINE_SHOW);
            }
            Settings.Global.putInt(context.getContentResolver(), SETTING_HIDE_GESTURE_LINE, HIDE_GESTURE_LINE_HIDE);
        } catch (Throwable e) {
            XposedLog.w(TAG, "applyGestureLineShielding E: " + e);
        }
    }

    private void restoreGestureLineState(Context context) {
        if (context == null || mGestureLineBeforeLock == null) return;
        try {
            Settings.Global.putInt(context.getContentResolver(), SETTING_HIDE_GESTURE_LINE, mGestureLineBeforeLock);
        } catch (Throwable e) {
            XposedLog.w(TAG, "restoreGestureLineState E: " + e);
        } finally {
            mGestureLineBeforeLock = null;
        }
    }

    public static void setLockApp(Context context, int id) {
        Settings.Global.putInt(context.getContentResolver(), SETTING_KEY_LOCK_APP, id);
    }

    public int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), SETTING_KEY_LOCK_APP);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private boolean isEnhancedShieldEnabled() {
        return PrefsBridge.getBoolean("system_framework_guided_access_status", false);
    }

    private void stopSystemLockTaskMode() {
        try {
            Class<?> activityTaskManager = findClassIfExists("android.app.ActivityTaskManager");
            if (activityTaskManager == null) return;
            Object service = callStaticMethod(activityTaskManager, "getService");
            callMethod(service, "stopSystemLockTaskMode");
        } catch (Throwable e) {
            XposedLog.e(TAG, "stopSystemLockTaskMode helper E: " + e);
        }
    }
}
