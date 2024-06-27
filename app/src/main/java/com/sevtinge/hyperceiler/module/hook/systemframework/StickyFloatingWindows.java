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

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.Pair;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedHelpers;

public class StickyFloatingWindows extends BaseHook {

    public static ConcurrentHashMap<String, Pair<Float, Rect>> fwApps = new ConcurrentHashMap<>();

    @Override
    public void init() {
        final List<String> fwBlackList = new ArrayList<>();
        fwBlackList.add("com.miui.securitycenter");
        fwBlackList.add("com.miui.home");
        Class<?> MiuiMultiWindowUtils = findClass("android.util.MiuiMultiWindowUtils");
        hookAllMethods("com.android.server.wm.ActivityStarterInjector", "modifyLaunchActivityOptionIfNeed", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                if (param.args.length != 8) return;
                Intent intent = (Intent) param.args[5];
                Object activityRecord = param.args[7];
                Intent recordIntent = (Intent) XposedHelpers.getObjectField(activityRecord, "intent");
                String recordPackageName = recordIntent.getComponent().getPackageName();
                String packageName = intent.getComponent().getPackageName();
                if (recordPackageName.equals(packageName)) {
                    // 如果是相同应用跳转就忽略,防止出现全屏应用跳转页面之后变成小窗的情况
                    return;
                }
                if (intent == null || intent.getComponent() == null) return;
                ActivityOptions options = (ActivityOptions) param.getResult();
                String pkgName = intent.getComponent().getPackageName();
                if (fwBlackList.contains(pkgName)) return;
                Context mContext;
                try {
                    mContext = (Context) XposedHelpers.getObjectField(param.args[0], "mContext");
                } catch (Throwable ignore) {
                    mContext = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.args[0], "mService"), "mContext");
                }
                if (fwApps.containsKey(pkgName)) {
                    try {
                        if (MiuiMultiWindowUtils == null) {
                            logE(TAG, StickyFloatingWindows.this.lpparam.packageName, "Cannot find MiuiMultiWindowUtils class");
                            return;
                        }
                        options = patchActivityOptions(mContext, options, pkgName, MiuiMultiWindowUtils);
                        param.setResult(options);
                    } catch (Throwable t) {
                        logW(TAG, "", t);
                    }
                }
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskSupervisor", "startActivityFromRecents", new MethodHook() {

            @Override
            protected void before(MethodHookParam param) {
                Object safeOptions = param.args[3];
                ActivityOptions options = (ActivityOptions) XposedHelpers.callMethod(safeOptions, "getOptions", param.thisObject);
                String pkgName = getTaskPackageName(param.thisObject, (int) param.args[2], options);
                if (fwBlackList.contains(pkgName)) return;
                if (fwApps.containsKey(pkgName)) {
                    Context mContext = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mService"), "mContext");
                    options = patchActivityOptions(mContext, options, pkgName, MiuiMultiWindowUtils);
                    XposedHelpers.setObjectField(safeOptions, "mOriginalOptions", options);
                    param.args[3] = safeOptions;
                    Intent intent = new Intent(ACTION_PREFIX + "dismissRecentsWhenFreeWindowOpen");
                    intent.putExtra("package", pkgName);
                    mContext.sendBroadcast(intent);
                }
            }
        });

        findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController$FreeFormReceiver", "onReceive", Context.class, Intent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                Intent intent = (Intent) param.args[1];
                String action = intent.getAction();
                if (action.equals("miui.intent.action_launch_fullscreen_from_freeform")) {
                    Object parentThis = XposedHelpers.getSurroundingThis(param.thisObject);
                    XposedHelpers.setAdditionalInstanceField(parentThis, "skipFreeFormStateClear", true);
                }
            }
        });

        hookAllMethods("com.android.server.wm.MiuiFreeFormGestureController", "notifyFullScreenWidnowModeStart", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                if (param.args.length != 3) return;
                String pkgName = (String) XposedHelpers.callMethod(param.args[1], "getStackPackageName");
                Object skipClear = XposedHelpers.getAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear");
                boolean skipFreeFormStateClear = false;
                if (skipClear != null) {
                    skipFreeFormStateClear = (boolean) skipClear;
                }
                if (!skipFreeFormStateClear) {
                    if (fwBlackList.contains(pkgName)) return;
                    if (fwApps.remove(pkgName) != null) {
                        storeFwAppsInSetting((Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mService"), "mContext"));
                    }
                } else {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear", false);
                }
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskManagerService", "launchSmallFreeFormWindow", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Object taskId = XposedHelpers.getObjectField(param.args[0], "taskId");
                Object mMiuiFreeFormManagerService = XposedHelpers.getObjectField(param.thisObject, "mMiuiFreeFormManagerService");
                Object miuiFreeFormActivityStack = XposedHelpers.callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", taskId);
                String pkgName = (String) XposedHelpers.callMethod(miuiFreeFormActivityStack, "getStackPackageName");
                if (fwBlackList.contains(pkgName)) return;
                if (!fwApps.containsKey(pkgName)) {
                    fwApps.put(pkgName, new Pair<>(0f, null));
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    storeFwAppsInSetting(mContext);
                }
            }
        });

        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new MethodHook() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            @Override
            protected void after(MethodHookParam param) {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                restoreFwAppsInSetting(mContext);
                Class<?> MiuiMultiWindowAdapter = findClass("android.util.MiuiMultiWindowAdapter", lpparam.classLoader);
                List<String> blackList = (List<String>) XposedHelpers.getStaticObjectField(MiuiMultiWindowAdapter, "FREEFORM_BLACK_LIST");
                blackList.clear();
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action.equals("miui.intent.action_launch_fullscreen_from_freeform")) {
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear", true);
                        }
                    }
                }, new IntentFilter("miui.intent.action_launch_fullscreen_from_freeform"));

                IntentFilter mFilter = new IntentFilter();
                mFilter.addAction(ACTION_PREFIX + "updateFwApps");
                mFilter.addAction(ACTION_PREFIX + "getFwApps");
                mFilter.addAction(ACTION_PREFIX + "removeFwApps");
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String pkgName = intent.getStringExtra("package");
                        switch (intent.getAction()) {
                            case ACTION_PREFIX + "updateFwApps":
                                float scale = intent.getFloatExtra("scale", 0f);
                                Rect rect = intent.getParcelableExtra("rect");
                                if (!fwApps.containsKey(pkgName)) {
                                    fwApps.put(pkgName, new Pair<>(scale, rect));
                                    storeFwAppsInSetting(context);
                                    return;
                                }
                                Pair<Float, Rect> oldPair = fwApps.get(pkgName);
                                if (scale == 0f) {
                                    scale = oldPair.first;
                                }
                                if (rect == null) {
                                    rect = oldPair.second;
                                }
                                fwApps.put(pkgName, new Pair<>(scale, rect));
                                storeFwAppsInSetting(context);
                                break;
                            case ACTION_PREFIX + "getFwApps":
                                syncFwApps(context);
                                break;
                            case ACTION_PREFIX + "removeFwApps":
                                if (pkgName != null && fwApps.remove(pkgName) != null) {
                                    storeFwAppsInSetting(context);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }, mFilter);
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskManagerService", "resizeTask", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String pkgName = getTaskPackageName(param.thisObject, (int) param.args[0]);
                if (pkgName != null) {
                    Object skipClear = XposedHelpers.getAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear");
                    boolean skipFreeFormStateClear = false;
                    if (skipClear != null) {
                        skipFreeFormStateClear = (boolean) skipClear;
                    }
                    if (skipFreeFormStateClear) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear", false);
                    } else {
                        if (fwBlackList.contains(pkgName)) return;
                        Object mMiuiFreeFormManagerService = XposedHelpers.getObjectField(param.thisObject, "mMiuiFreeFormManagerService");
                        Object miuiFreeFormActivityStack = XposedHelpers.callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", param.args[0]);
                        if (fwApps.containsKey(pkgName)) {
                            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            float sScale = (float) XposedHelpers.callMethod(miuiFreeFormActivityStack, "getFreeFormScale");
                            fwApps.put(pkgName, new Pair<>(sScale, new Rect((Rect) param.args[1])));
                            storeFwAppsInSetting(mContext);
                        }
                    }
                }
            }
        });
    }


    public static String getTaskPackageName(Object thisObject, int taskId) {
        return getTaskPackageName(thisObject, taskId, false, null);
    }

    public static String getTaskPackageName(Object thisObject, int taskId, ActivityOptions options) {
        return getTaskPackageName(thisObject, taskId, true, options);
    }

    public static String getTaskPackageName(Object thisObject, int taskId, boolean withOptions, ActivityOptions options) {
        Object mRootWindowContainer = XposedHelpers.getObjectField(thisObject, "mRootWindowContainer");
        if (mRootWindowContainer == null) return null;
        Object task = withOptions ?
                XposedHelpers.callMethod(mRootWindowContainer, "anyTaskForId", taskId, 2, options, true) :
                XposedHelpers.callMethod(mRootWindowContainer, "anyTaskForId", taskId, 0);
        if (task == null) return null;
        Intent intent = (Intent) XposedHelpers.getObjectField(task, "intent");
        return intent == null ? null : intent.getComponent().getPackageName();
    }

    public static String serializeFwApps() {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, Pair<Float, Rect>> entry : fwApps.entrySet()) {
            Pair<Float, Rect> val = entry.getValue();
            data.append(entry.getKey());
            data.append(":");
            data.append(val.first);
            data.append(":");
            data.append(val.second == null ? "-" : val.second.flattenToString());
            data.append("|");
        }
        return data.toString().replaceFirst("\\|$", "");
    }

    public static void unserializeFwApps(String data) {
        fwApps.clear();
        if (data == null || data.isEmpty()) return;
        String[] dataArr = data.split("\\|");
        for (String appData : dataArr) {
            if ("".equals(appData)) continue;
            String[] appDataArr = appData.split(":");
            fwApps.put(appDataArr[0], new Pair<>(Float.parseFloat(appDataArr[1]), "-".equals(appDataArr[2]) ? null : Rect.unflattenFromString(appDataArr[2])));
        }
    }

    public static void syncFwApps(Context context) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_PREFIX + "syncFwApps");
        intent.putExtra("fwApps", serializeFwApps());
        context.sendBroadcast(intent);
    }

    public static void storeFwAppsInSetting(Context context) {
        syncFwApps(context);
        Settings.Global.putString(context.getContentResolver(), ProjectApi.mAppModulePkg + ".fw.apps", serializeFwApps());
    }

    public static void restoreFwAppsInSetting(Context context) {
        unserializeFwApps(Settings.Global.getString(context.getContentResolver(), ProjectApi.mAppModulePkg + ".fw.apps"));
    }

    private static ActivityOptions patchActivityOptions(Context mContext, ActivityOptions options, String pkgName, Class<?> MiuiMultiWindowUtils) {
        if (options == null) options = ActivityOptions.makeBasic();
        XposedHelpers.callMethod(options, "setLaunchWindowingMode", 5);
        XposedHelpers.callMethod(options, "setMiuiConfigFlag", 2);

        Float scale;
        Rect rect;
        Pair<Float, Rect> values = fwApps.get(pkgName);
        if (values == null || values.first == 0f || values.second == null) {
            scale = 0.7f;
            rect = (Rect) XposedHelpers.callStaticMethod(MiuiMultiWindowUtils, "getFreeformRect", mContext);
        } else {
            scale = values.first;
            rect = values.second;
        }
        options.setLaunchBounds(rect);
        try {
            Object injector = XposedHelpers.callMethod(options, "getActivityOptionsInjector");
            XposedHelpers.callMethod(injector, "setFreeformScale", scale);
        } catch (Throwable ignore) {
        }
        return options;
    }
}
