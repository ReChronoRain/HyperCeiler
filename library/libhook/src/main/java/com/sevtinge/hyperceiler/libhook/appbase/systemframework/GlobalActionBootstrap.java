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
import static io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.HookHandle;

@SuppressLint("UnspecifiedRegisterReceiverFlag")
public class GlobalActionBootstrap extends BaseHook {
    private static volatile boolean sGlobalReceiverRegistered;
    private static volatile boolean sRestartReceiverRegistered;
    private static volatile int sContextualSearchPackageNameResId;

    private final BroadcastReceiver mGlobalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long token = Binder.clearCallingIdentity();
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
                } else if (GlobalActionBridge.ACTION_GO_HOME.equals(action)) {
                    handleGoHome(context);
                } else if (GlobalActionBridge.ACTION_SCREEN_CAPTURE.equals(action)) {
                    context.sendBroadcast(new Intent("android.intent.action.CAPTURE_SCREENSHOT"));
                } else if (GlobalActionBridge.ACTION_OPEN_POWER_MENU.equals(action)) {
                    callMethod(wms, "showGlobalActions");
                } else if (GlobalActionBridge.ACTION_LAUNCH_INTENT.equals(action)) {
                    handleLaunchIntent(context, intent);
                } else if (GlobalActionBridge.ACTION_FORCE_STOP_TOP_APP.equals(action)) {
                    handleForceStopTopApp(context);
                } else if ((BaseHook.ACTION_PREFIX + "StartGoogleCircleToSearch").equals(action)) {
                    handleStartGoogleCircleToSearch();
                }
            } catch (Throwable t) {
                AndroidLog.w(TAG, "system", "onReceive", t);
            } finally {
                Binder.restoreCallingIdentity(token);
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
        try {
            Class<?> rString = findClass("com.android.internal.R$string", null);
            sContextualSearchPackageNameResId = rString.getField("config_defaultContextualSearchPackageName").getInt(null);
            chainAllMethods("com.android.server.SystemServer", "deviceHasConfigString",
                new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        if ((Integer) chain.getArg(1) == sContextualSearchPackageNameResId) {
                            return true;
                        }
                        return chain.proceed();
                    }
                }
            );
        } catch (Throwable ignored) {
        }

        chainAllConstructors("com.android.server.accessibility.AccessibilityManagerService",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    Context globalContext = (Context) chain.getArg(0);
                    registerGlobalReceiver(globalContext);
                    return result;
                }
            }
        );

        chainAllMethods("com.android.server.policy.PhoneWindowManager", "init",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                    registerGlobalReceiver(context);
                    registerRestartReceiver(context);
                    return result;
                }
            }
        );
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
            filter.addAction(GlobalActionBridge.ACTION_GO_HOME);
            filter.addAction(GlobalActionBridge.ACTION_SCREEN_CAPTURE);
            filter.addAction(GlobalActionBridge.ACTION_OPEN_POWER_MENU);
            filter.addAction(GlobalActionBridge.ACTION_LAUNCH_INTENT);
            filter.addAction(GlobalActionBridge.ACTION_FORCE_STOP_TOP_APP);
            filter.addAction(BaseHook.ACTION_PREFIX + "StartGoogleCircleToSearch");
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

    private void handleGoHome(Context context) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(homeIntent);
    }

    private void handleForceStopTopApp(Context context) {
        String packageName = getTopPackageName(context);
        if (packageName == null || isProtectedPackage(packageName)) {
            return;
        }
        forceStopPackage(context, packageName);
    }

    private void handleStartGoogleCircleToSearch() {
        if (startContextualSearchService()) {
            return;
        }
        startVoiceInteractionSession();
    }

    private boolean startContextualSearchService() {
        ArrayList<HookHandle> hooks = new ArrayList<>();
        long token = Binder.clearCallingIdentity();
        try {
            ClassLoader classLoader = getClassLoader();
                Class<?> csmsClass = loadClass(
                    "com.android.server.contextualsearch.ContextualSearchManagerService",
                    classLoader
                );
            Method enforcePermission = csmsClass.getDeclaredMethod("enforcePermission", String.class);
            Method getContextualSearchPackageName = csmsClass.getDeclaredMethod("getContextualSearchPackageName");
            hooks.add(hookMethod(enforcePermission, new IMethodHook() {
                @Override
                public void before(io.github.kyuubiran.ezxhelper.xposed.common.HookParam param) {
                    param.setResult(null);
                }
            }));
            hooks.add(hookMethod(getContextualSearchPackageName, new IMethodHook() {
                @Override
                public void before(io.github.kyuubiran.ezxhelper.xposed.common.HookParam param) {
                    param.setResult("com.google.android.googlequicksearchbox");
                }
            }));

                Class<?> serviceManagerClass = loadClass("android.os.ServiceManager", classLoader);
            IBinder binder = (IBinder) serviceManagerClass.getMethod("getService", String.class)
                .invoke(null, "contextual_search");
            if (binder == null) {
                return false;
            }

                Class<?> stubClass = loadClass(
                    "android.app.contextualsearch.IContextualSearchManager$Stub",
                    classLoader
                );
            Object service = stubClass.getMethod("asInterface", IBinder.class).invoke(null, binder);
            if (service == null) {
                return false;
            }

            Object result = service.getClass().getMethod("startContextualSearch", int.class)
                .invoke(service, 1);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        } finally {
            for (HookHandle hook : hooks) {
                if (hook != null) {
                    hook.unhook();
                }
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean startVoiceInteractionSession() {
        try {
            Bundle bundle = new Bundle();
            bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime());
            bundle.putInt("omni.entry_point", 1);
            ClassLoader classLoader = getClassLoader();

            Class<?> serviceManagerClass = loadClass("android.os.ServiceManager", classLoader);
            IBinder binder = (IBinder) serviceManagerClass.getMethod("getService", String.class)
                .invoke(null, "voiceinteraction");
            if (binder == null) {
                return false;
            }

            Class<?> stubClass = loadClass(
                "com.android.internal.app.IVoiceInteractionManagerService$Stub",
                classLoader
            );
            Object service = stubClass.getMethod("asInterface", IBinder.class).invoke(null, binder);
            if (service == null) {
                return false;
            }

            try {
                Object result = service.getClass().getMethod(
                    "showSessionFromSession",
                    IBinder.class,
                    Bundle.class,
                    int.class,
                    String.class
                ).invoke(service, null, bundle, 7, "hyperOS_home");
                return !(result instanceof Boolean) || (Boolean) result;
            } catch (NoSuchMethodException ignored) {
                Object result = service.getClass().getMethod(
                    "showSessionFromSession",
                    IBinder.class,
                    Bundle.class,
                    int.class
                ).invoke(service, null, bundle, 7);
                return !(result instanceof Boolean) || (Boolean) result;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String getTopPackageName(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return null;
        }

        List<RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }

        RunningTaskInfo taskInfo = tasks.get(0);
        if (taskInfo.topActivity == null) {
            return null;
        }
        return taskInfo.topActivity.getPackageName();
    }

    private boolean isProtectedPackage(String packageName) {
        return "com.android.systemui".equals(packageName)
            || "com.miui.home".equals(packageName)
            || "com.miui.voiceassist".equals(packageName)
            || getPackageName().equals(packageName)
            || packageName.startsWith("com.android.inputmethod")
            || packageName.startsWith("com.google.android.inputmethod");
    }

    private static void forceStopPackage(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        callMethod(activityManager, "forceStopPackage", packageName);
    }
}
