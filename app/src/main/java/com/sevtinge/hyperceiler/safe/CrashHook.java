package com.sevtinge.hyperceiler.safe;

import android.app.ActivityOptions;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.module.base.tool.HookTool;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 推荐使用的方法，直接 Hook 系统，
 * 可能误报，但是很稳定。
 */
public class CrashHook extends HookTool {
    private static final String TAG = ITAG.TAG + ": CrashHook";
    private static HashMap<String, String> scopeMap = new HashMap<>();

    public CrashHook(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Exception {
        backgroundActivity(loadPackageParam.classLoader);
        init(loadPackageParam.classLoader);
        scopeMap = CrashData.scopeData();
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

    private void recordCrash(Context mContext, Object proc, ApplicationErrorReport.CrashInfo crashInfo,
                             String shortMsg, String longMsg, String stackTrace, long timeMillis, int pid, int uid) {
        PackageManager pm = mContext.getPackageManager();
        ApplicationInfo info = (ApplicationInfo) XposedHelpers.getObjectField(proc, "info");
        String pkg = info.packageName; // 包名
        String label = info.loadLabel(pm).toString(); // 应用名
        if (scopeMap.isEmpty()) scopeMap = CrashData.scopeData();
        if (scopeMap.get(pkg) == null) {
            return; // 不属于作用域范围
        }
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        arrayList.add(new CrashRecord(label, pkg, timeMillis, 0).toJSON()); // 添加本次崩溃记录
        ArrayList<JSONObject> data = getCrashRecord(mContext);
        if (!data.isEmpty()) {
            boolean isNewCrash = false; // 此条崩溃记录是否是全新的
            boolean needReplace = false; // 是否需要更新记录
            boolean isReport = false; // 是否需要报告崩溃
            boolean needRemoveList = false; // 是否更新列表
            ArrayList<Integer> removeList = new ArrayList<>();
            int remove = -1;
            JSONObject add = null;
            for (int i = 0; i < data.size(); i++) {
                remove = remove + 1;
                JSONObject jsonObject = data.get(i); // 读取
                long mTime = CrashRecord.getTime(jsonObject);
                if ((timeMillis - mTime) > 60000) {
                    removeList.add(remove);
                    needRemoveList = true;
                    if (removeList.size() == data.size()) {
                        break;
                    }
                    continue;
                }
                if (compare(jsonObject, label, pkg)) { // 如果全部匹配代表已经在数据库中
                    isNewCrash = false; // 不需要再添加
                    if ((timeMillis - mTime) < 10240) {
                        needReplace = true;
                        int count = CrashRecord.getCount(jsonObject);
                        if (count >= 2) { // 崩溃报告临界值
                            ArrayList<JSONObject> report = getReportCrash(mContext); // 获取已经存在的报告记录
                            ArrayList<JSONObject> newReport = new ArrayList<>();
                            if (!report.isEmpty()) {
                                boolean isNewReport = false; // 是否是新的报告
                                for (JSONObject j : report) {
                                    if (compare(j, label, pkg)) {
                                        isNewReport = false; // 不是新的直接跳过
                                        break;
                                    }
                                    isNewReport = true;
                                }
                                if (isNewReport) newReport.add(jsonObject); // 是新的则添加
                            } else {
                                newReport.add(jsonObject);
                            }
                            if (!newReport.isEmpty()) { // 非空则报告
                                try {
                                    report.addAll(newReport);
                                    reportCrash(mContext, report);
                                    reportCrashByIntent(mContext, longMsg, stackTrace, newReport.get(0));
                                } catch (Throwable e) {
                                    logE(TAG, "Report crash failed!" + e);
                                }
                                // logE(TAG, "new: " + report);
                            }
                        } else {
                            add = CrashRecord.putCount(jsonObject, count + 1); // 崩溃次数不足则累加
                        }
                    } else {
                        needReplace = true; // 超出时间则直接清楚本条记录
                    }
                    break;
                } else {
                    if (isReport) break; // 是否已经报告过了
                    isNewCrash = true;
                    ArrayList<JSONObject> report = getReportCrash(mContext);
                    if (!report.isEmpty()) {
                        for (JSONObject c : report) {
                            if (compare(c, label, pkg)) {
                                isReport = true; // 已经报告过了
                                break;
                            }
                        }
                    }
                }
            }
            if (needRemoveList) {
                for (int i = removeList.size() - 1; i >= 0; i--) {
                    int id = removeList.get(i);
                    data.remove(id);
                }
            }
            if (needReplace) {
                if (!(removeList.contains(remove) && needRemoveList))
                    data.remove(remove);
                if (add != null) data.add(add);
            }
            if (isNewCrash) {
                if (!isReport)
                    data.addAll(arrayList);
            }
            // logE(TAG, "data: " + data);
            setCrashRecord(mContext, data);
        } else {
            // logE(TAG, "arr: " + arrayList);
            setCrashRecord(mContext, arrayList);
        }
    }

    private boolean compare(JSONObject object, String label, String pkg) {
        String mLabel = CrashRecord.getLabel(object);
        String mPkg = CrashRecord.getPkg(object);
        return mLabel.equals(label) && mPkg.equals(pkg);
    }

    private void reportCrashByIntent(Context context, String longMsg, String stackTrace, JSONObject report) {
        if (scopeMap.isEmpty()) scopeMap = CrashData.scopeData();
        String pkg = CrashRecord.getPkg(report);
        String abbr = scopeMap.get(pkg);
        Intent intent = new Intent();
        intent.setAction("com.sevtinge.hyperceiler.crash.Service");
        intent.setPackage("com.sevtinge.hyperceiler");
        intent.putExtra("key_longMsg", longMsg);
        intent.putExtra("key_stackTrace", stackTrace);
        intent.putExtra("key_report", abbr);
        context.startService(intent);
    }

    private void reportCrash(Context mContext, ArrayList<JSONObject> data) {
        Settings.System.putString(mContext.getContentResolver(), "hyperceiler_crash_report", data.toString());
    }

    private ArrayList<JSONObject> getReportCrash(Context context) {
        String data = Settings.System.getString(context.getContentResolver(), "hyperceiler_crash_report");
        if (data == null || data.isEmpty() || data.equals("[]")) {
            return new ArrayList<>();
        }
        return CrashRecord.toArray(data);
    }

    private void setCrashRecord(Context mContext, ArrayList<JSONObject> data) {
        Settings.System.putString(mContext.getContentResolver(), "hyperceiler_crash_record_data", data.toString());
    }

    private ArrayList<JSONObject> getCrashRecord(Context mContext) {
        String data = Settings.System.getString(mContext.getContentResolver(), "hyperceiler_crash_record_data");
        if (data == null || data.isEmpty() || data.equals("[]")) {
            return new ArrayList<>();
        }
        return CrashRecord.toArray(data);
    }
}
