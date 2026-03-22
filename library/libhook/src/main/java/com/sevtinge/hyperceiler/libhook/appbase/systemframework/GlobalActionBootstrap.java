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
package com.sevtinge.hyperceiler.libhook.appbase.systemframework;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

@SuppressLint("UnspecifiedRegisterReceiverFlag")
public class GlobalActionBootstrap extends BaseHook {
    private static volatile boolean sGlobalReceiverRegistered;
    private static volatile boolean sRestartReceiverRegistered;

    private final BroadcastReceiver mGlobalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }

                Class<?> windowManagerGlobal = findClass("android.view.WindowManagerGlobal", null);
                Object wms = callStaticMethod(windowManagerGlobal, "getWindowManagerService");

                if (GlobalActionBridge.ACTION_TOGGLE_COLOR_INVERSION.equals(action)) {
                    handleToggleColorInversion(context);
                } else if (GlobalActionBridge.ACTION_LOCK_SCREEN.equals(action)) {
                    callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());
                    callMethod(wms, "lockNow", (Object) null);
                } else if (GlobalActionBridge.ACTION_GO_TO_SLEEP.equals(action)) {
                    callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());
                } else if (GlobalActionBridge.ACTION_SCREEN_CAPTURE.equals(action)) {
                    context.sendBroadcast(new Intent("android.intent.action.CAPTURE_SCREENSHOT"));
                } else if (GlobalActionBridge.ACTION_OPEN_POWER_MENU.equals(action)) {
                    callMethod(wms, "showGlobalActions");
                } else if (GlobalActionBridge.ACTION_LAUNCH_INTENT.equals(action)) {
                    handleLaunchIntent(context, intent);
                }
            } catch (Throwable t) {
                AndroidLog.w(TAG, "system", "onReceive", t);
            }
        }
    };

    private final BroadcastReceiver mRestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent == null || !GlobalActionBridge.ACTION_RESTART_APPS.equals(intent.getAction())) {
                    return;
                }
                forceStopPackage(context, intent.getStringExtra("packageName"));
            } catch (Exception e) {
                AndroidLog.e(TAG, "system", null, e);
            }
        }
    };

    @Override
    public void init() {
        chainAllConstructors("com.android.server.accessibility.AccessibilityManagerService", chain -> {
            Object result = chain.proceed();
            Context globalContext = (Context) chain.getArg(0);
            registerGlobalReceiver(globalContext);
            return result;
        });

        chainAllMethods("com.android.server.policy.PhoneWindowManager", "init", chain -> {
            Object result = chain.proceed();
            Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
            registerRestartReceiver(context);
            return result;
        });
    }

    private void registerGlobalReceiver(Context context) {
        if (context == null || sGlobalReceiverRegistered) {
            return;
        }
        synchronized (GlobalActionBootstrap.class) {
            if (sGlobalReceiverRegistered) {
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(GlobalActionBridge.ACTION_TOGGLE_COLOR_INVERSION);
            filter.addAction(GlobalActionBridge.ACTION_LOCK_SCREEN);
            filter.addAction(GlobalActionBridge.ACTION_GO_TO_SLEEP);
            filter.addAction(GlobalActionBridge.ACTION_SCREEN_CAPTURE);
            filter.addAction(GlobalActionBridge.ACTION_OPEN_POWER_MENU);
            filter.addAction(GlobalActionBridge.ACTION_LAUNCH_INTENT);
            context.registerReceiver(mGlobalReceiver, filter, Context.RECEIVER_EXPORTED);
            sGlobalReceiverRegistered = true;
        }
    }

    private void registerRestartReceiver(Context context) {
        if (context == null || sRestartReceiverRegistered) {
            return;
        }
        synchronized (GlobalActionBootstrap.class) {
            if (sRestartReceiverRegistered) {
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(GlobalActionBridge.ACTION_RESTART_APPS);
            context.registerReceiver(mRestartReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            sRestartReceiverRegistered = true;
        }
    }

    private void handleToggleColorInversion(Context context) {
        int enabled = Settings.Secure.getInt(
            context.getContentResolver(),
            "accessibility_display_inversion_enabled",
            0
        );
        int conflictProp = (int) proxySystemProperties("getInt", "ro.df.effect.conflict", 0, null);
        int conflictProp2 = (int) proxySystemProperties("getInt", "ro.vendor.df.effect.conflict", 0, null);
        boolean hasConflict = conflictProp == 1 || conflictProp2 == 1;
        Object displayFeatureManager = callStaticMethod(findClass("miui.hardware.display.DisplayFeatureManager", null), "getInstance");
        if (hasConflict && enabled == 0) {
            callMethod(displayFeatureManager, "setScreenEffect", 15, 1);
        }
        Settings.Secure.putInt(
            context.getContentResolver(),
            "accessibility_display_inversion_enabled",
            enabled == 0 ? 1 : 0
        );
        if (hasConflict && enabled != 0) {
            callMethod(displayFeatureManager, "setScreenEffect", 15, 0);
        }
    }

    private void handleLaunchIntent(Context context, Intent intent) {
        Intent launchIntent = intent.getParcelableExtra("intent", Intent.class);
        if (launchIntent == null) {
            return;
        }

        int user = 0;
        if (launchIntent.hasExtra("user")) {
            user = launchIntent.getIntExtra("user", 0);
            launchIntent.removeExtra("user");
        }

        if (user != 0) {
            callMethod(context, "startActivityAsUser", launchIntent, newInstance(UserHandle.class, user));
        } else {
            context.startActivity(launchIntent);
        }
    }

    private static void forceStopPackage(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        callMethod(activityManager, "forceStopPackage", packageName);
    }
}
