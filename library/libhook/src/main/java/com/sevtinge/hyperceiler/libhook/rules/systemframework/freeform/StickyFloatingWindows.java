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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getAdditionalInstanceField;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getSurroundingThis;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setAdditionalInstanceField;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.Pair;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class StickyFloatingWindows extends BaseHook {

    public static ConcurrentHashMap<String, Pair<Float, Rect>> fwApps = new ConcurrentHashMap<>();

    @Override
    public void init() {
        final List<String> fwBlackList = new ArrayList<>();
        fwBlackList.add("com.miui.securitycenter");
        fwBlackList.add("com.miui.home");
        Class<?> MiuiMultiWindowUtils = findClass("android.util.MiuiMultiWindowUtils");
        hookAllMethods("com.android.server.wm.ActivityStarterInjector", "modifyLaunchActivityOptionIfNeed", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (param.getArgs().length != 8) return;
                Intent intent = (Intent) param.getArgs()[5];
                Object activityRecord = param.getArgs()[7];
                Intent recordIntent = (Intent) getObjectField(activityRecord, "intent");
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
                    mContext = (Context) getObjectField(param.getArgs()[0], "mContext");
                } catch (Throwable ignore) {
                    mContext = (Context) getObjectField(getObjectField(param.getArgs()[0], "mService"), "mContext");
                }
                if (fwApps.containsKey(pkgName)) {
                    try {
                        if (MiuiMultiWindowUtils == null) {
                            XposedLog.e(TAG, getPackageName(), "Cannot find MiuiMultiWindowUtils class");
                            return;
                        }
                        options = patchActivityOptions(mContext, options, pkgName, MiuiMultiWindowUtils);
                        param.setResult(options);
                    } catch (Throwable t) {
                        XposedLog.w(TAG, "", t);
                    }
                }
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskSupervisor", "startActivityFromRecents", new IMethodHook() {

            @Override
            public void before(BeforeHookParam param) {
                Object safeOptions = param.getArgs()[3];
                ActivityOptions options = (ActivityOptions) callMethod(safeOptions, "getOptions", param.getThisObject());
                String pkgName = getTaskPackageName(param.getThisObject(), (int) param.getArgs()[2], options);
                if (fwBlackList.contains(pkgName)) return;
                if (fwApps.containsKey(pkgName)) {
                    Context mContext = (Context) getObjectField(getObjectField(param.getThisObject(), "mService"), "mContext");
                    options = patchActivityOptions(mContext, options, pkgName, MiuiMultiWindowUtils);
                    setObjectField(safeOptions, "mOriginalOptions", options);
                    param.getArgs()[3] = safeOptions;
                    Intent intent = new Intent(ACTION_PREFIX + "dismissRecentsWhenFreeWindowOpen");
                    intent.putExtra("package", pkgName);
                    mContext.sendBroadcast(intent);
                }
            }
        });

        findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController$FreeFormReceiver", "onReceive", Context.class, Intent.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                Intent intent = (Intent) param.getArgs()[1];
                String action = intent.getAction();
                if (action.equals("miui.intent.action_launch_fullscreen_from_freeform")) {
                    Object parentThis = getSurroundingThis(param.getThisObject());
                    setAdditionalInstanceField(parentThis, "skipFreeFormStateClear", true);
                }
            }
        });

        hookAllMethods("com.android.server.wm.MiuiFreeFormGestureController", "notifyFullScreenWidnowModeStart", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (param.getArgs().length != 3) return;
                String pkgName = (String) callMethod(param.getArgs()[1], "getStackPackageName");
                Object skipClear = getAdditionalInstanceField(param.getThisObject(), "skipFreeFormStateClear");
                boolean skipFreeFormStateClear = false;
                if (skipClear != null) {
                    skipFreeFormStateClear = (boolean) skipClear;
                }
                if (!skipFreeFormStateClear) {
                    if (fwBlackList.contains(pkgName)) return;
                    if (fwApps.remove(pkgName) != null) {
                        storeFwAppsInSetting((Context) getObjectField(getObjectField(param.getThisObject(), "mService"), "mContext"));
                    }
                } else {
                    setAdditionalInstanceField(param.getThisObject(), "skipFreeFormStateClear", false);
                }
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskManagerService", "launchSmallFreeFormWindow", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Object taskId = getObjectField(param.getArgs()[0], "taskId");
                Object mMiuiFreeFormManagerService = getObjectField(param.getThisObject(), "mMiuiFreeFormManagerService");
                Object miuiFreeFormActivityStack = callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", taskId);
                String pkgName = (String) callMethod(miuiFreeFormActivityStack, "getStackPackageName");
                if (fwBlackList.contains(pkgName)) return;
                if (!fwApps.containsKey(pkgName)) {
                    fwApps.put(pkgName, new Pair<>(0f, null));
                    Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                    storeFwAppsInSetting(mContext);
                }
            }
        });

        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService", "onSystemReady", new IMethodHook() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            @Override
            public void after(AfterHookParam param) {
                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                restoreFwAppsInSetting(mContext);
                Class<?> MiuiMultiWindowAdapter = findClass("android.util.MiuiMultiWindowAdapter");
                List<String> blackList = (List<String>) getStaticObjectField(MiuiMultiWindowAdapter, "FREEFORM_BLACK_LIST");
                blackList.clear();
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action.equals("miui.intent.action_launch_fullscreen_from_freeform")) {
                            setAdditionalInstanceField(param.getThisObject(), "skipFreeFormStateClear", true);
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
                        String action = intent.getAction();
                        if ((ACTION_PREFIX + "updateFwApps").equals(action)) {
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
                        } else if ((ACTION_PREFIX + "getFwApps").equals(action)) {
                            syncFwApps(context);
                        } else if ((ACTION_PREFIX + "removeFwApps").equals(action)) {
                            if (pkgName != null && fwApps.remove(pkgName) != null) {
                                storeFwAppsInSetting(context);
                            }
                        }
                    }
                }, mFilter);
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskManagerService", "resizeTask", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                String pkgName = getTaskPackageName(param.getThisObject(), (int) param.getArgs()[0]);
                if (pkgName != null) {
                    Object skipClear = getAdditionalInstanceField(param.getThisObject(), "skipFreeFormStateClear");
                    boolean skipFreeFormStateClear = false;
                    if (skipClear != null) {
                        skipFreeFormStateClear = (boolean) skipClear;
                    }
                    if (skipFreeFormStateClear) {
                        setAdditionalInstanceField(param.getThisObject(), "skipFreeFormStateClear", false);
                    } else {
                        if (fwBlackList.contains(pkgName)) return;
                        Object mMiuiFreeFormManagerService = getObjectField(param.getThisObject(), "mMiuiFreeFormManagerService");
                        Object miuiFreeFormActivityStack = callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", param.getArgs()[0]);
                        if (fwApps.containsKey(pkgName)) {
                            Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                            float sScale = (float) callMethod(miuiFreeFormActivityStack, "getFreeFormScale");
                            fwApps.put(pkgName, new Pair<>(sScale, new Rect((Rect) param.getArgs()[1])));
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
        Object mRootWindowContainer = getObjectField(thisObject, "mRootWindowContainer");
        if (mRootWindowContainer == null) return null;
        Object task = withOptions ?
                callMethod(mRootWindowContainer, "anyTaskForId", taskId, 2, options, true) :
                callMethod(mRootWindowContainer, "anyTaskForId", taskId, 0);
        if (task == null) return null;
        Intent intent = (Intent) getObjectField(task, "intent");
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

    static ActivityOptions patchActivityOptions(Context mContext, ActivityOptions options, String pkgName, Class<?> MiuiMultiWindowUtils) {
        if (options == null) options = ActivityOptions.makeBasic();
        callMethod(options, "setLaunchWindowingMode", 5);
        callMethod(options, "setMiuiConfigFlag", 2);

        Float scale;
        Rect rect;
        Pair<Float, Rect> values = fwApps.get(pkgName);
        if (values == null || values.first == 0f || values.second == null) {
            scale = 0.7f;
            rect = (Rect) callStaticMethod(MiuiMultiWindowUtils, "getFreeformRect", mContext);
        } else {
            scale = values.first;
            rect = values.second;
        }
        options.setLaunchBounds(rect);
        try {
            Object injector = callMethod(options, "getActivityOptionsInjector");
            callMethod(injector, "setFreeformScale", scale);
        } catch (Throwable ignore) {
        }
        return options;
    }
}
