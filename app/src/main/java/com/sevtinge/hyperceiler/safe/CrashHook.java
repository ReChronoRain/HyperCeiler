package com.sevtinge.hyperceiler.safe;

import android.app.ActivityOptions;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.module.base.tool.HookTool;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;

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
                        logE(TAG, "context: " + mContext + " pkg: " + mContext.getPackageName() + " proc: " + proc + " crash: " + crashInfo + " short: " + shortMsg
                                + " long: " + longMsg + " stack: " + stackTrace + " time: " + timeMillis + " pid: " + callingPid + " uid: " + callingUid);
                        recordCrash(mContext, proc, crashInfo, shortMsg, longMsg, stackTrace, timeMillis, callingPid, callingUid);
                    }
                }
        );
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
        Intent intent = getIntent(abbr, stringBuilder);
        mContext.startService(intent);
    }

    @NonNull
    private Intent getIntent(String abbr, StringBuilder stringBuilder) {
        Intent intent = new Intent();
        intent.setAction("com.sevtinge.hyperceiler.crash.Service");
        intent.setPackage("com.sevtinge.hyperceiler");
        intent.putExtra("key_longMsg", longMsg);
        intent.putExtra("key_throwClassName", throwClassName);
        intent.putExtra("key_throwFileName", throwFileName);
        intent.putExtra("key_throwLineNumber", throwLineNumber);
        intent.putExtra("key_throwMethodName", throwMethodName);
        intent.putExtra("key_stackTrace", stackTrace);
        intent.putExtra("key_pkg", abbr);
        intent.putExtra("key_all", stringBuilder.toString());
        return intent;
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
