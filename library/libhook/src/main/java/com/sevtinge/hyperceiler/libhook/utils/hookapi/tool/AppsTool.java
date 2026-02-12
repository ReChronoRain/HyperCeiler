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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool;

import static com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mPrefsMap;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.net.Uri;
import android.os.FileObserver;
import android.os.UserHandle;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellInit;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.github.libxposed.api.XposedModuleInterface;

public class AppsTool {

    private static final String TAG = "AppsTool";

    public static final int FLAG_ALL = ContextUtils.FLAG_ALL;
    public static final int FLAG_CURRENT_APP = ContextUtils.FLAG_CURRENT_APP;
    public static final int FlAG_ONLY_ANDROID = ContextUtils.FlAG_ONLY_ANDROID;

    public static LruCache<String, Bitmap> memoryCache = new LruCache<>(
        (int) (Runtime.getRuntime().maxMemory() / 1024) / 2
    ) {
        @Override
        protected int sizeOf(String key, Bitmap icon) {
            return icon != null ? icon.getAllocationByteCount() / 1024 : 130 * 130 * 4 / 1024;
        }
    };

    @SuppressLint("StaticFieldLeak")
    public static TextView mPct = null;

    private static FileObserver sFileObserver = null;
    private static final Executor sPermissionExecutor = Executors.newSingleThreadExecutor();

    public static synchronized Context getProtectedContext(Context context) {
        return context.createDeviceProtectedStorageContext();
    }

    public static Resources getModuleRes(Context context) {
        return ResourcesTool.getInstance().loadModuleRes(context);
    }

    public static Context findContext(@ContextUtils.Duration int flag) {
        try {
            return switch (flag) {
                case FLAG_ALL -> currentApplication() != null ? currentApplication() : getSystemContext();
                case FLAG_CURRENT_APP -> currentApplication();
                case FlAG_ONLY_ANDROID -> getSystemContext();
                default -> null;
            };
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static Context currentApplication() {
        Class<?> clazz = EzxHelpUtils.findClass("android.app.ActivityThread", null);
        return (Application) EzxHelpUtils.callStaticMethod(clazz, "currentApplication");
    }

    private static Context getSystemContext() {
        Class<?> clazz = EzxHelpUtils.findClass("android.app.ActivityThread", null);
        Object thread = EzxHelpUtils.callStaticMethod(clazz, "currentActivityThread");
        if (thread == null) return null;

        Context context = (Context) EzxHelpUtils.callMethod(thread, "getSystemContext");
        if (context == null) {
            context = (Context) EzxHelpUtils.callMethod(thread, "getSystemUiContext");
        }
        return context;
    }

    public static boolean isDarkMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("DiscouragedApi")
    public static int getSystemBackgroundColor(Context context) {
        int black = Color.BLACK;
        int white = Color.WHITE;
        try {
            Resources res = context.getResources();
            black = res.getColor(res.getIdentifier("black", "color", "miui"), context.getTheme());
            white = res.getColor(res.getIdentifier("white", "color", "miui"), context.getTheme());
        } catch (Throwable ignore) {
        }
        return isDarkMode(context) ? black : white;
    }

    public static void applyShimmer(TextView title) {
        if (title.getPaint().getShader() != null) return;
        int width = title.getResources().getDisplayMetrics().widthPixels;
        Shader shimmer = new LinearGradient(
            0, 0, width, 0,
            new int[]{0xFF5DA5FF, 0xFF9B8AFB, 0xFFD176F2, 0xFFFE88B2, 0xFFD176F2, 0xFF9B8AFB},
            new float[]{0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f},
            Shader.TileMode.REPEAT
        );
        Matrix matrix = new Matrix();
        matrix.setTranslate(0, 0);
        shimmer.setLocalMatrix(matrix);
        title.getPaint().setShader(shimmer);
    }

    public static void initPct(ViewGroup container, int source) {
        Resources res = container.getContext().getResources();
        if (mPct == null) {
            Context context = container.getContext();
            float density = res.getDisplayMetrics().density;

            mPct = new TextView(context);
            mPct.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            mPct.setGravity(Gravity.CENTER);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = Math.round(mPrefsMap.getInt("system_ui_others_showpct_top", 54) * density *
                (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 0.7f : 1.0f));
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            mPct.setPadding(
                Math.round(20 * density),
                Math.round(10 * density),
                Math.round(18 * density),
                Math.round(12 * density)
            );
            mPct.setLayoutParams(lp);

            try {
                Resources modRes = getModuleRes(context);
                mPct.setTextColor(ColorStateList.valueOf(Color.parseColor("#FFFFFFFF")));
                mPct.setBackground(modRes.getDrawable(R.drawable.input_background, context.getTheme()));
            } catch (Throwable err) {
                XposedLog.e("ShowPct", err);
            }

            if (mPrefsMap.getBoolean("system_showpct_use_blur")) {
                try {
                    int blurRadius = isDarkMode(getSystemContext()) ? 220 : 320;
                    int alpha = isDarkMode(getSystemContext()) ? 140 : 160;

                    MiBlurUtils.clearMiBackgroundBlendColor(mPct);
                    MiBlurUtils.setPassWindowBlurEnabled(mPct, true);
                    MiBlurUtils.setMiViewBlurMode(mPct, 1);
                    MiBlurUtils.setMiBackgroundBlurMode(mPct, 1);
                    MiBlurUtils.setMiBackgroundBlurRadius(mPct, blurRadius);
                    MiBlurUtils.addMiBackgroundBlendColor(mPct, Color.argb(alpha, 0, 0, 0), 101);
                } catch (Throwable e) {
                    XposedLog.e("ShowPct", e);
                }
            }
            container.addView(mPct);
        }
        mPct.setTag(source);
        mPct.setVisibility(View.GONE);
    }

    public static void removePct(TextView mPctText) {
        if (mPctText != null) {
            mPctText.setVisibility(View.GONE);
            ViewGroup p = (ViewGroup) mPctText.getParent();
            p.removeView(mPctText);
            mPct = null;
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

    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    public static void fixPermissionsAsync(Context context) {
        sPermissionExecutor.execute(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            setFilePermissions(context.getDataDir());
            setFilePermissions(new File(PrefsUtils.getSharedPrefsPath()));
            setFilePermissions(new File(PrefsUtils.getSharedPrefsFile()));
        });
    }

    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void setFilePermissions(File file) {
        if (file != null && file.exists()) {
            file.setExecutable(true, false);
            file.setReadable(true, false);
            file.setWritable(true, false);
        }
    }


    public static void registerFileObserver(Context context) {
        try {
            sFileObserver = new FileObserver(new File(PrefsUtils.getSharedPrefsPath()), FileObserver.CLOSE_WRITE) {
                @Override
                public void onEvent(int event, String path) {
                    AppsTool.fixPermissionsAsync(context);
                }
            };
            sFileObserver.startWatching();
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to start FileObserver!");
        }
    }

    public static void requestBackup(Context context) {
        new BackupManager(context).dataChanged();
    }

    private static String getCallerMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : stackTrace) {
            if (el != null && el.getClassName().startsWith(ProjectApi.mAppModulePkg + ".module"))
                return el.getMethodName();
        }
        return stackTrace[4].getMethodName();
    }

    public static String getPackageVersionName(XposedModuleInterface.PackageLoadedParam param) {
        try {
            PackageManager pm = findContext(FlAG_ONLY_ANDROID).getPackageManager();
            PackageInfo pi = pm.getPackageInfo(param.getPackageName(), 0);
            return pi != null ? pi.versionName : "";
        } catch (Throwable e) {
            AndroidLog.e("getPackageVersionName", param.getPackageName(), e.toString());
            return "";
        }
    }

    public static int getPackageVersionCode(XposedModuleInterface.PackageLoadedParam param) {
        try {
            PackageManager pm = findContext(FlAG_ONLY_ANDROID).getPackageManager();
            PackageInfo pi = pm.getPackageInfo(param.getPackageName(), 0);
            return Math.toIntExact(pi != null ? pi.getLongVersionCode() : -1);
        } catch (Throwable e) {
            AndroidLog.e("getPackageVersionCode", param.getPackageName(), e.toString());
            return -1;
        }
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
                if (user != 0) {
                    Object userHandle = EzxHelpUtils.newInstance(UserHandle.class, user);
                    EzxHelpUtils.callMethod(context, "startActivityAsUser", intent, userHandle);
                } else {
                    context.startActivity(intent);
                }
            } catch (Throwable t2) {
                AndroidLog.e(TAG, "openAppInfo" + t2);
            }
        }
    }

    public static boolean killApps(String packageName) {
        return killApps(new String[]{packageName});
    }

    public static boolean killApps(String... packageNames) {
        return killApps(packageNames, 15);
    }

    public static boolean killApps(String[] packageNames, int signal) {
        if (packageNames == null || packageNames.length == 0) {
            AndroidLog.e(TAG, "packageNames is null or empty");
            return false;
        }
        boolean hasSuccess = false;
        for (String pkg : packageNames) {
            if (pkg == null || pkg.trim().isEmpty()) {
                AndroidLog.e(TAG, "packageName item is null or empty");
                continue;
            }
            boolean shellResult;
            try {
                shellResult =
                    ShellInit.getShell().add("pid=$(pgrep -f \"" + pkg + "\" | grep -v $$)")
                        .add("if [[ $pid == \"\" ]]; then")
                        .add(" pids=\"\"")
                        .add(" pid=$(ps -A -o PID,ARGS=CMD | grep \"" + pkg + "\" | grep -v \"grep\")")
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
                        .add("killed=0")
                        .add("if [[ $pid != \"\" ]]; then")
                        .add(" for i in $pid; do")
                        .add("  kill -s " + signal + " $i &>/dev/null")
                        .add("  if [[ $? -eq 0 ]]; then killed=1; fi")
                        .add(" done")
                        .add(" if [[ $killed -eq 0 ]]; then echo \"No Permission!\"; fi")
                        .add("else")
                        .add(" echo \"No Find Pid!\"")
                        .add("fi").over().sync().isResult();
            } catch (Exception e) {
                AndroidLog.e(TAG, "Exception: " + e.getMessage() + " package: " + pkg);
                continue;
            }
            ArrayList<String> outPut = ShellInit.getShell().getOutPut();
            ArrayList<String> error = ShellInit.getShell().getError();
            if (shellResult) {
                if (outPut != null && !outPut.isEmpty()) {
                    String firstLine = outPut.getFirst();
                    if ("No Find Pid!".equals(firstLine)) {
                        AndroidLog.w(TAG, "Didn't find a pid that can kill: " + pkg);
                    } else if ("No Permission!".equals(firstLine)) {
                        AndroidLog.w(TAG, "No permission to kill process: " + pkg);
                    } else {
                        hasSuccess = true;
                    }
                } else {
                    hasSuccess = true;
                }
            } else {
                AndroidLog.e(TAG, "Shell failed, errorMsg: " + error + " package: " + pkg);
            }
            if (error != null && !error.isEmpty()) {
                AndroidLog.e(TAG, "Shell error output: " + error + " package: " + pkg);
            }
        }
        return hasSuccess;
    }
}

