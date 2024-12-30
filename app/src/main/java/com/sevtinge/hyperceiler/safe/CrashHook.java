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
package com.sevtinge.hyperceiler.safe;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.app.ActivityOptions;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.data.AppCrashInfo;
import com.sevtinge.hyperceiler.data.AppErrorsData;
import com.sevtinge.hyperceiler.data.ProcessInfo;
import com.sevtinge.hyperceiler.module.base.tool.HookTool;
import com.sevtinge.hyperceiler.receiver.CrashReceiver;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 推荐使用的方法，直接 Hook 系统，
 * 可能误报，但是很稳定。
 */
public class CrashHook extends HookTool {
    private static final String TAG = ITAG.TAG + ": CrashHook";
    private static HashMap<String, String> scopeMap = new HashMap<>();
    private static HashMap<String, String> swappedMap = new HashMap<>();
    private final String ACTION_APP_CRASH = "hyperceiler.intent.action.APP_CRASH";

    public CrashHook(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Exception {
        backgroundActivity(loadPackageParam.classLoader);
        init(loadPackageParam.classLoader);
        scopeMap = CrashData.scopeData();
        swappedMap = CrashData.swappedData();
    }

    public void init(ClassLoader classLoader) throws Exception {
        Class<?> appError = findClassIfExists("com.android.server.am.AppErrors", classLoader);
        if (appError == null) {
            throw new ClassNotFoundException("No such 'com.android.server.am.AppErrors' classLoader: " + classLoader);
        }
        Method hookError = null;
        for (Method error : appError.getDeclaredMethods()) {
            if ("handleAppCrashInActivityController".equals(error.getName()))
                if (error.getReturnType().equals(boolean.class)) {
                    hookError = error;
                    break;
                }
        }
        if (hookError == null) {
            throw new NoSuchMethodException("No such Method: handleAppCrashInActivityController, ClassLoader: " + classLoader);
        }

        hookMethod(hookError, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Object proc = param.args[0];
                        ApplicationErrorReport.CrashInfo crashInfo = (ApplicationErrorReport.CrashInfo) param.args[1];
                        String shortMsg = (String) param.args[2];
                        String longMsg = (String) param.args[3];
                        String stackTrace = (String) param.args[4];
                        long timeMillis = (long) param.args[5];
                        int callingPid = (int) param.args[6];
                        int callingUid = (int) param.args[7];
                        logE("CrashHook", "context: " + mContext + " pkg: " + mContext.getPackageName() + " proc: " + proc + " crash: " + crashInfo + " short: " + shortMsg
                                + " long: " + longMsg + " stack: " + stackTrace + " time: " + timeMillis + " pid: " + callingPid + " uid: " + callingUid);
                        recordCrash(mContext, proc, crashInfo, shortMsg, longMsg, stackTrace, timeMillis, callingPid, callingUid);

                        //XposedBridge.log(crashInfo.throwClassName + " :" + crashInfo.exceptionMessage);
                    }
                }
        );
/*
        findAndHookMethod("com.android.server.wm.BackgroundActivityStartController", "checkCrossUidActivitySwitchFromBelow", "com.android.server.wm.ActivityRecord", int.class, "com.android.server.wm.BackgroundActivityStartController$BlockActivityStart", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(param.args[2]);
            }
        });*/
    }

    private void backgroundActivity(ClassLoader classLoader) {
        // 允许哈皮露后台启动界面
        try {
            findAndHookMethod("com.android.server.wm.ActivityStarter", classLoader, "shouldAbortBackgroundActivityStart",
                    int.class, int.class, String.class, int.class, int.class,
                    "com.android.server.wm.WindowProcessController", "com.android.server.am.PendingIntentRecord",
                    boolean.class, Intent.class, ActivityOptions.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            String pkg = (String) param.args[2];
                            if (pkg == null) return;
                            if (ProjectApi.mAppModulePkg.equals(pkg)) {
                                param.setResult(false);
                            }
                        }
                    }
            );
        } catch (Throwable e) {
            if (isMoreAndroidVersion(35)) {
                findAndHookMethod("com.android.server.wm.BackgroundActivityStartController", classLoader, "checkBackgroundActivityStart",
                        int.class, int.class, String.class, int.class, int.class,
                        "com.android.server.wm.WindowProcessController", "com.android.server.am.PendingIntentRecord",
                        "android.app.BackgroundStartPrivileges", "com.android.server.wm.ActivityRecord", Intent.class, ActivityOptions.class,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                String pkg = (String) param.args[2];
                                AndroidLogUtils.logI("jzskjizdfblkjnxdfboknxffb", pkg);
                                if (pkg == null) return;
                                if (ProjectApi.mAppModulePkg.equals(pkg)) {
                                    findAndHookMethod("com.android.server.wm.BackgroundActivityStartController$BalVerdict", classLoader, "allows", new MethodHook() {
                                                @Override
                                                protected void before(MethodHookParam param) {
                                                    AndroidLogUtils.logI("jzskjizdfblkjnxdfboknxffb", "1");
                                                    param.setResult(true);
                                                }
                                            }
                                    );
                                    /*Object balAllowDefault = XposedHelpers.getStaticObjectField(
                                            XposedHelpers.findClass("com.android.server.wm.BackgroundActivityStartController$BalVerdict",
                                                    lpparam.classLoader),
                                            "BAL_ALLOW_DEFAULT"
                                    );
                                    AndroidLogUtils.logI("jzskjizdfblkjnxdfboknxffb", "2");
                                    param.setResult(balAllowDefault);*/
                                }
                            }
                        }
                );
            } else {
                findAndHookMethod("com.android.server.wm.BackgroundActivityStartController", classLoader, "checkBackgroundActivityStart",
                        int.class, int.class, String.class, int.class, int.class,
                        "com.android.server.wm.WindowProcessController", "com.android.server.am.PendingIntentRecord",
                        "android.app.BackgroundStartPrivileges", Intent.class, ActivityOptions.class,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                String pkg = (String) param.args[2];
                                if (pkg == null) return;
                                if (ProjectApi.mAppModulePkg.equals(pkg)) {
                                    param.setResult(1);
                                }
                            }
                        }
                );
            }
        }
        hookAllMethods("com.android.server.wm.ActivityStarterImpl", classLoader,
                "isAllowedStartActivity",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        int count = -1;
                        for (Object clz : param.args) {
                            count = count + 1;
                            if (clz instanceof String) {
                                break;
                            }
                        }
                        String pkg = (String) param.args[count];
                        if (pkg == null) return;
                        if (ProjectApi.mAppModulePkg.equals(pkg)) {
                            param.setResult(true);
                        }
                    }
                }
        );
    }

    private String mPkg;
    private String longMsg;
    private String stackTrace;
    private String throwClassName;
    private String throwFileName;
    private int throwLineNumber;
    private String throwMethodName;
    private long timeMillis;
    private Context mContext;
    private ArrayList<JSONObject> data = new ArrayList<>();
    private final ArrayList<JSONObject> updateCount = new ArrayList<>();
    private final ArrayList<JSONObject> reportData = new ArrayList<>();

    private void recordCrash(Context mContext, Object proc, ApplicationErrorReport.CrashInfo crashInfo,
                             String shortMsg, String longMsg, String stackTrace, long timeMillis, int pid, int uid) {
        ApplicationInfo info = (ApplicationInfo) XposedHelpers.getObjectField(proc, "info");
        mPkg = info.packageName;
        this.mContext = mContext;
        this.timeMillis = timeMillis;
        this.longMsg = longMsg;
        this.stackTrace = stackTrace;
        throwClassName = crashInfo.throwClassName;
        throwFileName = crashInfo.throwFileName;
        throwLineNumber = crashInfo.throwLineNumber;
        throwMethodName = crashInfo.throwMethodName;
        if (!isScopeApp()) return;
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        ArrayList<JSONObject> report = getReportCrash();
        arrayList.add(new CrashRecord(mPkg, timeMillis, 0).toJSON());
        data = getCrashRecord();
        longTimeRemove();
        if (!data.isEmpty()) {
            Iterator<JSONObject> iterator = data.iterator();
            while (iterator.hasNext()) {
                JSONObject oldData = iterator.next();
                boolean isReport = false;
                for (JSONObject next : report) {
                    if (compare(next)) {
                        isReport = true;
                        break;
                    }
                }
                if (isReport) break;
                if (compare(oldData)) {
                    long time = CrashRecord.getTime(oldData);
                    if ((timeMillis - time) < 10240) {
                        int count = CrashRecord.getCount(oldData);
                        if (count >= 2) {
                            reportData.add(oldData);
                        } else {
                            updateCount.add(CrashRecord.putParam(oldData, timeMillis, count + 1));
                        }
                        iterator.remove();
                    } else {
                        iterator.remove();
                    }
                }
            }
            setCrashRecord(data);
            if (!updateCount.isEmpty()) {
                data.addAll(updateCount);
                setCrashRecord(data);
                updateCount.clear();
            }
            if (!reportData.isEmpty()) {
                reportData.addAll(report);
                reportCrashByIntent(reportData);
                reportData.clear();
            }
        } else {
            setCrashRecord(arrayList);
        }
    }

    private boolean isScopeApp() {
        if (scopeMap.isEmpty()) scopeMap = CrashData.scopeData();
        return scopeMap.get(mPkg) != null;
    }

    private void longTimeRemove() {
        Iterator<JSONObject> it = data.iterator();
        while (it.hasNext()) {
            JSONObject jsonObject = it.next();
            long time = CrashRecord.getTime(jsonObject);
            if ((timeMillis - time) > 60000) {
                it.remove();
            }
        }
    }

    private boolean compare(JSONObject object) {
        // String mLabel = CrashRecord.getLabel(object);
        String mPkg = CrashRecord.getPkg(object);
        return mPkg.equals(this.mPkg);
    }

    private void reportCrashByIntent(ArrayList<JSONObject> report) {
        if (scopeMap.isEmpty()) scopeMap = CrashData.scopeData();
        StringBuilder stringBuilder = new StringBuilder();
        for (JSONObject j : report) {
            String b = scopeMap.get(CrashRecord.getPkg(j));
            if (stringBuilder.length() == 0) stringBuilder.append(b);
            else stringBuilder.append(",").append(b);
        }

        String abbr = scopeMap.get(CrashRecord.getPkg(report.get(0)));

        ShellInit.init();
        ShellInit.getShell().run("setprop persist.hyperceiler.crash.report " + "\"" + stringBuilder + "\"").sync();

        Intent intent1 = getIntent(abbr);
        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent1);

        /*Intent intent = getIntent(abbr, stringBuilder);
        mContext.startService(intent);*/
    }

    private Intent getIntent(String abbr) {
        Intent intent1 = new Intent();
        intent1.setPackage("com.sevtinge.hyperceiler");
        intent1.setClassName("com.sevtinge.hyperceiler", "com.sevtinge.hyperceiler.safe.CrashActivity");
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent1.putExtra("key_longMsg", longMsg);
        intent1.putExtra("key_stackTrace", stackTrace);
        intent1.putExtra("key_throwClassName", throwClassName);
        intent1.putExtra("key_throwFileName", throwFileName);
        intent1.putExtra("key_throwLineNumber", throwLineNumber);
        intent1.putExtra("key_throwMethodName", throwMethodName);
        intent1.putExtra("key_pkg", abbr);
        AndroidLogUtils.logI("iafjnsdkjnsdlvkzdv", "3");
        return intent1;
    }

    private ArrayList<JSONObject> getReportCrash() {
        ArrayList<String> stringData = CrashData.getReportCrashProp();
        ArrayList<JSONObject> objects = new ArrayList<>();
        if (swappedMap.isEmpty()) swappedMap = CrashData.swappedData();
        for (String s : stringData) {
            String pkg = swappedMap.get(s);
            objects.add(new CrashRecord(pkg, -1, -1).toJSON());
        }
        return objects;
    }

    private void setCrashRecord(ArrayList<JSONObject> data) {
        Settings.System.putString(mContext.getContentResolver(), "hyperceiler_crash_record_data", data.toString());
    }

    private ArrayList<JSONObject> getCrashRecord() {
        String data = Settings.System.getString(mContext.getContentResolver(), "hyperceiler_crash_record_data");
        if (data == null || data.isEmpty() || data.equals("[]")) {
            return new ArrayList<>();
        }
        return CrashRecord.toArray(data);
    }
}
