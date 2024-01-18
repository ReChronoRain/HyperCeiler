package com.sevtinge.hyperceiler;

import android.app.ActivityOptions;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.sevtinge.hyperceiler.utils.PropUtils;
import com.sevtinge.hyperceiler.utils.hook.HookUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 推荐使用的方法，直接 Hook 系统，
 * 可能误报，但是很稳定。
 */
public class CrashHook extends HookUtils {
    private final String path = "/sdcard/Download/hy_crash";
    private static final String TAG = com.sevtinge.hyperceiler.callback.TAG.TAG + ": CrashHook";

    public CrashHook(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Exception {
        init(loadPackageParam.classLoader);
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
        logE("CrashHook", "get method: " + hookError.getName());
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
                        if ("com.sevtinge.hyperceiler".equals(pkg)) {
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
                        if ("com.sevtinge.hyperceiler".equals(pkg)) {
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
                    if ("com.sevtinge.hyperceiler".equals(pkg)) {
                        param.setResult(true);
                    }
                }
            }
        );
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

    private void recordCrash(Context mContext, Object proc, ApplicationErrorReport.CrashInfo crashInfo,
                             String shortMsg, String longMsg, String stackTrace, long timeMillis, int pid, int uid) {
        PackageManager pm = mContext.getPackageManager();
        ApplicationInfo info = (ApplicationInfo) XposedHelpers.getObjectField(proc, "info");
        String pkg = info.packageName; // 包名
        String label = info.loadLabel(pm).toString(); // 应用名
        logE(TAG, "pkg: " + pkg + " l: " + label);
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        arrayList.add(new CrashData(label, pkg, timeMillis, 0).toJSON());
        ArrayList<JSONObject> getReport = getReportCrash(mContext);
        ArrayList<String> prop = getReportCrashProp();
        boolean equal = false;
        if (getReport.size() == prop.size()) {
            if (!getReport.isEmpty() && !prop.isEmpty()) {
                for (JSONObject j : getReport) {
                    for (int k = 0; k < prop.size(); k++) {
                        if (CrashData.getPkg(j).equals(prop.get(k))) {
                            equal = true;
                            break;
                        } else {
                            equal = false;
                        }
                    }
                    if (!equal) break;
                }
            } else equal = true;
            if (!equal) {
                logE(TAG, "dont equal 1 S: " + getReport + " P: " + prop);
                // reportCrash(mContext, new ArrayList<>());
                reportCrashWithIntent(mContext, false, getReport);
            }
        } else if (getReport.size() != prop.size()) {
            logE(TAG, "dont equal 2 S: " + getReport + " P: " + prop);
            reportCrashWithIntent(mContext, false, getReport);
        }
        ArrayList<JSONObject> data = getCrashData(mContext);
        if (!data.isEmpty()) {
            boolean needAdd = false;
            boolean needRm = false;
            boolean isReport = false;
            int rm = -1;
            JSONObject add = null;
            for (int i = 0; i < data.size(); i++) {
                rm = rm + 1;
                JSONObject jsonObject = data.get(i);
                String mLabel = CrashData.getLabel(jsonObject);
                String mPkg = CrashData.getPkg(jsonObject);
                long mTime = CrashData.getTime(jsonObject);
                if (mLabel.equals(label) && mPkg.equals(pkg)) {
                    needAdd = false;
                    if ((timeMillis - mTime) < 10240) {
                        needRm = true;
                        int count = CrashData.getCount(jsonObject);
                        if (count >= 3) {
                            ArrayList<JSONObject> report = getReportCrash(mContext);
                            ArrayList<JSONObject> newReport = new ArrayList<>();
                            if (!report.isEmpty()) {
                                boolean isNewReport = false;
                                for (JSONObject j : report) {
                                    String l = CrashData.getLabel(j);
                                    String p = CrashData.getPkg(j);
                                    if (l.equals(label) && p.equals(pkg)) {
                                        isNewReport = false;
                                        break;
                                    }
                                    isNewReport = true;
                                }
                                if (isNewReport) newReport.add(jsonObject);
                            } else {
                                newReport.add(jsonObject);
                            }
                            if (!newReport.isEmpty()) {
                                try {
                                    report.addAll(newReport);
                                    reportCrash(mContext, report);
                                    reportCrashWithIntent(mContext, true, report);
                                    // writFile(report);
                                    // reportCrashProp(newReport); // 会申请Root
                                } catch (Throwable e) {
                                    logE(TAG, "report E: " + e);
                                }
                                logE(TAG, "new: " + report);
                            }
                        } else {
                            add = CrashData.putCount(jsonObject, count + 1);
                        }
                    } else {
                        needRm = true;
                    }
                    break;
                } else {
                    if (isReport) break;
                    needAdd = true;
                    ArrayList<JSONObject> report = getReportCrash(mContext);
                    if (!report.isEmpty()) {
                        for (JSONObject c : report) {
                            String cPkg = CrashData.getPkg(c);
                            String cLabel = CrashData.getLabel(c);
                            if (cPkg.equals(pkg) && cLabel.equals(label)) {
                                isReport = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (needRm) {
                data.remove(rm);
                if (add != null) data.add(add);
            }
            if (needAdd) {
                if (!isReport)
                    data.addAll(arrayList);
            }
            logE(TAG, "data: " + data);
            setCrashData(mContext, data);
        } else {
            logE(TAG, "arr: " + arrayList);
            setCrashData(mContext, arrayList);
        }

    }

    // private void reportCrashWithIntent(Context context) {
    //     Intent intent = new Intent();
    //     intent.setAction("com.sevtinge.hyperceiler.Crash.Service");
    //     intent.setPackage("com.sevtinge.hyperceiler");
    //     intent.setComponent(new ComponentName("com.sevtinge.hyperceiler", "com.sevtinge.hyperceiler.CrashService"));
    //     context.startService(intent);
    // }

    private void reportCrashWithIntent(Context context, boolean equal, ArrayList<JSONObject> cover) {
        /*ArrayList<JSONObject> report = new ArrayList<>();
        if (!cover.isEmpty()) {
            for (JSONObject j : cover) {
                report.add(new CrashData(CrashData.getPkg(j), CrashData.getCount(j)).toJSONSmall());
            }
        }*/
        ArrayList<String> report = new ArrayList<>();
        if (!cover.isEmpty()) {
            for (JSONObject j : cover) {
                report.add(CrashData.getPkg(j));
            }
        }
        Intent intent = new Intent();
        intent.setAction("com.sevtinge.hyperceiler.Crash.Service");
        intent.setPackage("com.sevtinge.hyperceiler");
        intent.putExtra("key_equal", equal);
        intent.putExtra("key_cover", report.toString());
//        intent.setComponent(new ComponentName("com.sevtinge.hyperceiler", "com.sevtinge.hyperceiler.CrashService"));
        context.startService(intent);
    }

    private ArrayList<String> getReportCrashProp() {
        String data = PropUtils.getProp("persist.hyperceiler.crash.report", "[]");
        if (data.equals("[]") || data.equals("")) {
            return new ArrayList<>();
        }
        // return CrashData.toArray(data);
        data = data.replace("[", "").replace("]", "").replace(" ", "");
        String[] sp = data.split(",");
        return new ArrayList<>(Arrays.asList(sp));
    }

    /*private void reportCrashProp(ArrayList<JSONObject> data) {
        PropUtils.setProp("persist.hyperceiler.crash.report", data.toString());
    }

    private boolean haveFile() {
        File file = new File(path);
        return file.exists();
    }

    private void writFile(ArrayList<JSONObject> jsonObjects) {
        if (haveFile()) {
            try (BufferedWriter writer = new BufferedWriter(new
                FileWriter(path, false))) {
                writer.write(jsonObjects.toString());
            } catch (IOException e) {
                logE(TAG, "writeFile: " + e);
            }
        } else {
            logE(TAG, "Dont have file: " + path);
        }
    }

    private ArrayList<JSONObject> readFile() {
        try (BufferedReader reader = new BufferedReader(new
            FileReader(path))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonString = builder.toString();
            if (jsonString.equals("") || jsonString.equals("[]")) {
                return new ArrayList<>();
            }
            return CrashData.toArray(jsonString);
        } catch (IOException e) {
            logE(TAG, "readFile: " + e);
        }
        return new ArrayList<>();
    }*/

    private void reportCrash(Context mContext, ArrayList<JSONObject> data) {
        Settings.System.putString(mContext.getContentResolver(), "hyperceiler_crash_report", data.toString());
    }

    private ArrayList<JSONObject> getReportCrash(Context context) {
        String data = Settings.System.getString(context.getContentResolver(), "hyperceiler_crash_report");
        if (data == null || data.equals("") || data.equals("[]")) {
            return new ArrayList<>();
        }
        return CrashData.toArray(data);
    }

    private void setCrashData(Context mContext, ArrayList<JSONObject> data) {
        Settings.System.putString(mContext.getContentResolver(), "hyperceiler_crash_record_data", data.toString());
    }

    private ArrayList<JSONObject> getCrashData(Context mContext) {
        String data = Settings.System.getString(mContext.getContentResolver(), "hyperceiler_crash_record_data");
        if (data == null || data.equals("") || data.equals("[]")) {
            return new ArrayList<>();
        }
        return CrashData.toArray(data);
    }

    public static class CrashData {
        public String label;
        public String pkg;
        public long time;
        public int count;

        public CrashData(String l, String p, long t, int c) {
            label = l;
            pkg = p;
            time = t;
            count = c;
        }

        public CrashData(String p, int c) {
            pkg = p;
            count = c;
        }

        public static String getLabel(JSONObject jsonObject) {
            try {
                return jsonObject.getString("l");
            } catch (JSONException e) {
                logE(TAG, "getLabel E: " + e);
            }
            return "null";
        }

        public static String getPkg(JSONObject jsonObject) {
            try {
                return jsonObject.getString("p");
            } catch (JSONException e) {
                logE(TAG, "getPkg E: " + e);
            }
            return "null";
        }

        public static long getTime(JSONObject jsonObject) {
            try {
                return jsonObject.getLong("t");
            } catch (JSONException e) {
                logE(TAG, "getTime E: " + e);
            }
            return -1L;
        }

        public static int getCount(JSONObject jsonObject) {
            try {
                return jsonObject.getInt("c");
            } catch (JSONException e) {
                logE(TAG, "getCount E: " + e);
            }
            return -1;
        }

        public static JSONObject putCount(JSONObject jsonObject, int count) {
            try {
                return jsonObject.put("c", count);
            } catch (JSONException e) {
                logE(TAG, "putCount E: " + e);
            }
            return new JSONObject();
        }

        public JSONObject toJSON() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("l", label);
                jsonObject.put("p", pkg);
                jsonObject.put("t", time);
                jsonObject.put("c", count);
                return jsonObject;
            } catch (JSONException e) {
                logE(TAG, "toJSON E: " + e);
            }
            return jsonObject;
        }

        public JSONObject toJSONSmall() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("p", pkg);
                jsonObject.put("c", count);
                return jsonObject;
            } catch (JSONException e) {
                logE(TAG, "toJSON E: " + e);
            }
            return jsonObject;
        }

        public static ArrayList<JSONObject> toArray(String json) {
            try {
                ArrayList<JSONObject> list = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    list.add(obj);
                }
                return list;
            } catch (Exception e) {
                logE(TAG, "toArray E: " + e);
            }
            return new ArrayList<>();
        }
    }
}
