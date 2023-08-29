package com.sevtinge.cemiuiler.module.systemframework;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.Pair;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.LogUtils;

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
            protected void after(MethodHookParam param) throws Throwable {
                if (param.args.length != 8) return;
                Intent intent = (Intent) param.args[5];
                if (intent == null || intent.getComponent() == null) return;
                ActivityOptions options = (ActivityOptions) param.getResult();
                int windowingMode = options == null ? -1 : (int) XposedHelpers.callMethod(options, "getLaunchWindowingMode");
                String pkgName = intent.getComponent().getPackageName();
                if (fwBlackList.contains(pkgName)) return;
                Context mContext;
                try {
                    mContext = (Context) XposedHelpers.getObjectField(param.args[0], "mContext");
                } catch (Throwable ignore) {
                    mContext = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.args[0], "mService"), "mContext");
                }
                if (windowingMode != 5 && fwApps.containsKey(pkgName)) {
                    try {
                        if (MiuiMultiWindowUtils == null) {
                            LogUtils.logXp(TAG, "Cannot find MiuiMultiWindowUtils class");
                            return;
                        }
                        options = patchActivityOptions(mContext, options, pkgName, MiuiMultiWindowUtils);
                        param.setResult(options);
                    } catch (Throwable t) {
                        LogUtils.log(TAG, t);
                    }
                } else if (windowingMode == 5 && !fwApps.containsKey(pkgName)) {
                    fwApps.put(pkgName, new Pair<>(0f, null));
                    storeFwAppsInSetting(mContext);
                }
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskSupervisor", "startActivityFromRecents", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Object safeOptions = param.args[3];
                ActivityOptions options = (ActivityOptions) XposedHelpers.callMethod(safeOptions, "getOptions", param.thisObject);
                int windowingMode = options == null ? -1 : (int) XposedHelpers.callMethod(options, "getLaunchWindowingMode");
                String pkgName = getTaskPackageName(param.thisObject, (int) param.args[2], options);
                if (fwBlackList.contains(pkgName)) return;
                if (windowingMode == 5 && pkgName != null) {
                    fwApps.put(pkgName, new Pair<>(0f, null));
                    Context mContext = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mService"), "mContext");
                    storeFwAppsInSetting(mContext);
                }
            }

            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object safeOptions = param.args[3];
                ActivityOptions options = (ActivityOptions) XposedHelpers.callMethod(safeOptions, "getOptions", param.thisObject);
                int windowingMode = options == null ? -1 : (int) XposedHelpers.callMethod(options, "getLaunchWindowingMode");
                String pkgName = getTaskPackageName(param.thisObject, (int) param.args[2], options);
                if (fwBlackList.contains(pkgName)) return;
                if (windowingMode != 5 && fwApps.containsKey(pkgName)) {
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

        findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController$FreeFormReceiver", "onReceive", new Object[]{Context.class, Intent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[1];
                String action = intent.getAction();
                if (action == "miui.intent.action_launch_fullscreen_from_freeform") {
                    Object parentThis = XposedHelpers.getSurroundingThis(param.thisObject);
                    XposedHelpers.setAdditionalInstanceField(parentThis, "skipFreeFormStateClear", true);
                }
            }
        }});

        hookAllMethods("com.android.server.wm.MiuiFreeFormGestureController", "notifyFullScreenWidnowModeStart", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
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
            protected void after(MethodHookParam param) throws Throwable {
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
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                restoreFwAppsInSetting(mContext);
                Class<?> MiuiMultiWindowAdapter = findClass("android.util.MiuiMultiWindowAdapter", lpparam.classLoader);
                List<String> blackList = (List<String>) XposedHelpers.getStaticObjectField(MiuiMultiWindowAdapter, "FREEFORM_BLACK_LIST");
                blackList.clear();
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action == "miui.intent.action_launch_fullscreen_from_freeform") {
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "skipFreeFormStateClear", true);
                        }
                    }
                }, new IntentFilter("miui.intent.action_launch_fullscreen_from_freeform"));
            }
        });

        hookAllMethods("com.android.server.wm.ActivityTaskManagerService", "resizeTask", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
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
        if (data == null || "".equals(data)) return;
        String[] dataArr = data.split("\\|");
        for (String appData : dataArr) {
            if ("".equals(appData)) continue;
            String[] appDataArr = appData.split(":");
            fwApps.put(appDataArr[0], new Pair<>(Float.parseFloat(appDataArr[1]), "-".equals(appDataArr[2]) ? null : Rect.unflattenFromString(appDataArr[2])));
        }
    }

    public static void storeFwAppsInSetting(Context context) {
        Settings.Global.putString(context.getContentResolver(), Helpers.mAppModulePkg + ".fw.apps", serializeFwApps());
    }

    public static void restoreFwAppsInSetting(Context context) {
        unserializeFwApps(Settings.Global.getString(context.getContentResolver(), Helpers.mAppModulePkg + ".fw.apps"));
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
            LogUtils.log(ignore);
        }
        return options;
    }
}
