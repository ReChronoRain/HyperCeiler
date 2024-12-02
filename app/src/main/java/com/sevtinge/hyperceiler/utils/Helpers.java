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
package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.UserHandle;
import android.util.Log;
import android.util.LruCache;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fan.appcompat.app.AlertDialog;

public class Helpers {

    private static final String TAG = "Helpers";

    public static boolean isModuleActive = false;
    public static int XposedVersion = 0;

    public static final int REQUEST_PERMISSIONS_BACKUP = 1;
    public static final int REQUEST_PERMISSIONS_RESTORE = 2;


    public static LruCache<String, Bitmap> memoryCache = new LruCache<>((int) (Runtime.getRuntime().maxMemory() / 1024) / 2) {
        @Override
        protected int sizeOf(String key, Bitmap icon) {
            if (icon != null) {
                return icon.getAllocationByteCount() / 1024;
            } else {
                return 130 * 130 * 4 / 1024;
            }
        }
    };

    public static synchronized Context getProtectedContext(Context context) {
        return context.createDeviceProtectedStorageContext();
    }

    public static synchronized Context getModuleContext(Context context) throws Throwable {
        return getModuleContext(context, null);
    }

    public static synchronized Context getModuleContext(Context context, Configuration config) throws Throwable {
        Context mModuleContext;
        mModuleContext = context.createPackageContext(ProjectApi.mAppModulePkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static synchronized Resources getModuleRes(Context context) throws Throwable {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    }

    public static boolean checkStorageReadable(Activity activity) {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY) || state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            DialogHelper.showDialog(activity, "警告！", "无法访问任何合适的存储空间");
            return false;
        }
    }

    public static class MimeType {
        public static int IMAGE = 1;
        public static int AUDIO = 2;
        public static int VIDEO = 4;
        public static int DOCUMENT = 8;
        public static int ARCHIVE = 16;
        public static int LINK = 32;
        public static int OTHERS = 64;
        public static int ALL = IMAGE | AUDIO | VIDEO | DOCUMENT | ARCHIVE | LINK | OTHERS;
    }

    public static boolean isDarkMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("DiscouragedApi")
    public static int getSystemBackgroundColor(Context context) {
        int black = Color.BLACK;
        int white = Color.WHITE;
        try {
            black = context.getResources().getColor(context.getResources().getIdentifier("black", "color", "miui"), context.getTheme());
            white = context.getResources().getColor(context.getResources().getIdentifier("white", "color", "miui"), context.getTheme());
        } catch (Throwable ignore) {
        }
        return isDarkMode(context) ? black : white;
    }

    public static void applyShimmer(TextView title) {
        if (title.getPaint().getShader() != null) return;
        int width = title.getResources().getDisplayMetrics().widthPixels;
        Shader shimmer = new LinearGradient(0, 0, width, 0, new int[]{0xFF5DA5FF, 0xFF9B8AFB, 0xFFD176F2, 0xFFFE88B2, 0xFFD176F2, 0xFF9B8AFB}, new float[]{0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f}, Shader.TileMode.REPEAT);
        Matrix matrix = new Matrix();
        matrix.setTranslate(0, 0);
        shimmer.setLocalMatrix(matrix);
        title.getPaint().setShader(shimmer);
    }

    // Permissions 权限
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    public static void fixPermissionsAsync(Context context) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Thread.sleep(500);
            } catch (Throwable ignore) {
            }
            File pkgFolder = context.getDataDir();
            if (pkgFolder.exists()) {
                pkgFolder.setExecutable(true, false);
                pkgFolder.setReadable(true, false);
                pkgFolder.setWritable(true, false);
            }
            File sharedPrefsFolder = new File(PrefsUtils.getSharedPrefsPath());
            if (sharedPrefsFolder.exists()) {
                sharedPrefsFolder.setExecutable(true, false);
                sharedPrefsFolder.setReadable(true, false);
                sharedPrefsFolder.setWritable(true, false);
            }
            File sharedPrefsFile = new File(PrefsUtils.getSharedPrefsFile());
            if (sharedPrefsFile.exists()) {
                sharedPrefsFile.setReadable(true, false);
                sharedPrefsFile.setExecutable(true, false);
                sharedPrefsFile.setWritable(true, false);
            }
        });
    }

    public static void registerFileObserver(Context context) {
        try {
            FileObserver mFileObserver = new FileObserver(new File(PrefsUtils.getSharedPrefsPath()), FileObserver.CLOSE_WRITE) {
                @Override
                public void onEvent(int event, String path) {
                    Helpers.fixPermissionsAsync(context);
                }
            };
            mFileObserver.startWatching();
        } catch (Throwable t) {
            Log.e("prefs", "Failed to start FileObserver!");
        }
    }

    public static void requestBackup(Context context) {
        new BackupManager(context).dataChanged();
    }

    private static String getCallerMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : stackTrace)
            if (el != null && el.getClassName().startsWith(ProjectApi.mAppModulePkg + ".module"))
                return el.getMethodName();
        return stackTrace[4].getMethodName();
    }

    public static ActivityOptions makeFreeformActivityOptions(Context context, String str) {

        /*ActivityOptions activityOptions;
        try {
            activityOptions = ReflectUtils.callStaticObjectMethod(Class.forName("android.util.MiuiMultiWindowUtils"), ActivityOptions.class, "getActivityOptions", new Class[]{Context.class, String.class, Boolean.TYPE, Boolean.TYPE}, context, str, true, false);
        } catch (Exception e) {
            logD(TAG, "MiuiMultiWindowUtils getActivityOptions error", e);
            activityOptions = null;
        }

        if (activityOptions != null) {
            return activityOptions;
        }


        ActivityOptions makeBasic = ActivityOptions.makeBasic();
        ReflectUtils.callObjectMethod("android.app.ActivityOptions", "setLaunchWindowingMode", new Class[]{int.class}, 5);
        Rect rect = (Rect) ReflectUtils.callObjectMethod("android.util.MiuiMultiWindowUtils", "getFreeformRect", new Class[]{Context.class}, new Object[]{context});
        makeBasic.setLaunchBounds(rect);
        return makeBasic;*/
        return null;
    }

    public static void openAppInfo(Context context, String pkg, int user) {
        try {
            Intent intent = new Intent("miui.intent.action.APP_MANAGER_APPLICATION_DETAIL");
            intent.setPackage("com.miui.securitycenter");
            intent.putExtra("package_name", pkg);
            if (user != 0) intent.putExtra("miui.intent.extra.USER_ID", user);
            context.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                intent.setData(Uri.parse("package:" + pkg));
                if (user != 0)
                    XposedHelpers.callMethod(context, "startActivityAsUser", intent, XposedHelpers.newInstance(UserHandle.class, user));
                else
                    context.startActivity(intent);
            } catch (Throwable t2) {
                logE(TAG, "openAppInfo" + t2);
            }
        }
    }

    public static String getPackageVersionName(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            //XposedLogUtils.logI("getPackageVersionName", lpparam.packageName + " versionName is " + versionName);
            return (String) XposedHelpers.getObjectField(pkg, "mVersionName");
        } catch (Throwable e) {
            //XposedLogUtils.logW("getPackageVersionName", e);
            return "null";
        }
    }

    public static int getPackageVersionCode(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            //XposedLogUtils.logI("getPackageVersionCode", lpparam.packageName + " versionCode is " + versionCode);
            return XposedHelpers.getIntField(pkg, "mVersionCode");
        } catch (Throwable e) {
            //XposedLogUtils.logW("getPackageVersionCode", e);
            return -1;
        }
    }

    public static void checkXposedActivateState(Context context) {
        if (!isModuleActive) DialogHelper.showXposedActivateDialog(context);
    }

    public static void doRestart(Context context, String[] packageName, boolean isRestartSystem) {
        boolean result = false;
        boolean pid = true;
        if (isRestartSystem) {
            result = ShellInit.getShell().run("reboot").sync().isResult();
        } else {
            if (packageName != null) {
                for (String packageGet : packageName) {
                    if (packageGet == null) {
                        continue;
                    }
                    // String test = "XX";
                    // ShellUtils.CommandResult commandResult = ShellUtils.execCommand("{ [[ $(pgrep -f '" + packageGet +
                    //     "' | grep -v $$) != \"\" ]] && { pkill -l 9 -f \"" + packageGet +
                    //     "\"; }; } || { echo \"kill error\"; }", true, true);

                    boolean getResult =
                            ShellInit.getShell().add("pid=$(pgrep -f \"" + packageGet + "\" | grep -v $$)")
                                    .add("if [[ $pid == \"\" ]]; then")
                                    .add(" pids=\"\"")
                                    .add(" pid=$(ps -A -o PID,ARGS=CMD | grep \"" + packageGet + "\" | grep -v \"grep\")")
                                    .add("  for i in $pid; do")
                                    .add("   if [[ $(echo $i | grep '[0-9]' 2>/dev/null) != \"\" ]]; then")
                                    .add("    if [[ $pids == \"\" ]]; then")
                                    .add("      pids=$i")
                                    .add("    else")
                                    .add("      pids=\"$pids $i\"")
                                    .add("    fi")
                                    .add("   fi")
                                    .add("  done")
                                    .add("fi")
                                    .add("if [[ $pids != \"\" ]]; then")
                                    .add(" pid=$pids")
                                    .add("fi")
                                    .add("if [[ $pid != \"\" ]]; then")
                                    .add(" for i in $pid; do")
                                    .add("  kill -s 15 $i &>/dev/null")
                                    .add(" done")
                                    .add("else")
                                    .add(" echo \"No Find Pid!\"")
                                    .add("fi").over().sync().isResult();
                    ArrayList<String> outPut = ShellInit.getShell().getOutPut();
                    ArrayList<String> error = ShellInit.getShell().getError();

                    if (getResult) {
                        if (!outPut.isEmpty()) {
                            if (outPut.get(0).equals("No Find Pid!")) {
                                pid = false;
                            } else {
                                result = true;
                            }
                        } else result = true;
                    } else
                        AndroidLogUtils.logE("doRestart: ", "result: " + ShellInit.getShell().getResult() +
                                " errorMsg: " + error + " package: " + packageGet);

                }
            } else {
                AndroidLogUtils.logE("doRestart: ", "packageName is null");
            }
            // result = ShellUtils.getResultBoolean("pkill -l 9 -f " + packageName, true);
        }
        if (!result) {
            new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(R.string.tip)
                    .setMessage(isRestartSystem ? R.string.reboot_failed :
                            pid ? R.string.kill_failed : R.string.pid_failed)
                    .setHapticFeedbackEnabled(true)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }
}
