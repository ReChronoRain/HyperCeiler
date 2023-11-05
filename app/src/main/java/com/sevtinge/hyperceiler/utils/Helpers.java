package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.utils.log.AndroidLogUtils.LogD;
import static com.sevtinge.hyperceiler.utils.log.AndroidLogUtils.LogI;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.util.LruCache;
import android.widget.TextView;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import moralnorm.internal.utils.ReflectUtils;

public class Helpers {

    private static final String TAG = "Helpers";

    @SuppressLint("StaticFieldLeak")
    public static Context mModuleContext = null;
    public static boolean isModuleActive = false;
    public static int XposedVersion = 0;

    public static String mAppModulePkg = BuildConfig.APPLICATION_ID;

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

    public static boolean isDackMode(Context context) {
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
        return isDackMode(context) ? black : white;
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

    private static String getCallerMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : stackTrace)
            if (el != null && el.getClassName().startsWith(mAppModulePkg + ".module"))
                return el.getMethodName();
        return stackTrace[4].getMethodName();
    }

    public static Context findContext() {
        Context context = null;
        try {
            context = (Application) XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentApplication");
            if (context == null) {
                Object currentActivityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
                if (currentActivityThread != null)
                    context = (Context) XposedHelpers.callMethod(currentActivityThread, "getSystemContext");
            }
        } catch (Throwable ignore) {
        }
        return context;
    }

    public static synchronized Context getModuleContext(Context context) throws Throwable {
        return getModuleContext(context, null);
    }

    public static synchronized Context getModuleContext(Context context, Configuration config) throws Throwable {
        if (mModuleContext == null)
            mModuleContext = context.createPackageContext(mAppModulePkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static synchronized Resources getModuleRes(Context context) throws Throwable {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    }

    public static ActivityOptions makeFreeformActivityOptions(Context context, String str) {

        ActivityOptions activityOptions;
        try {
            activityOptions = (ActivityOptions) ReflectUtils.callStaticObjectMethod(Class.forName("android.util.MiuiMultiWindowUtils"), ActivityOptions.class, "getActivityOptions", new Class[]{Context.class, String.class, Boolean.TYPE, Boolean.TYPE}, new Object[]{context, str, true, false});
        } catch (Exception e) {
            LogD(TAG, "MiuiMultiWindowUtils getActivityOptions error", e);
            activityOptions = null;
        }

        if (activityOptions != null) {
            return activityOptions;
        }


        ActivityOptions makeBasic = ActivityOptions.makeBasic();
        ReflectUtils.callObjectMethod("android.app.ActivityOptions", "setLaunchWindowingMode", new Class[]{int.class}, 5);
        Rect rect = (Rect) ReflectUtils.callObjectMethod("android.util.MiuiMultiWindowUtils", "getFreeformRect", new Class[]{Context.class}, new Object[]{context});
        makeBasic.setLaunchBounds(rect);
        return makeBasic;
    }

    public static class SharedPrefObserver extends ContentObserver {

        enum PrefType {
            Any, String, StringSet, Integer, Boolean
        }

        PrefType prefType;
        Context ctx;
        String prefName;
        String prefDefValueString;
        int prefDefValueInt;
        boolean prefDefValueBool;

        public SharedPrefObserver(Context context, android.os.Handler handler) {
            super(handler);
            ctx = context;
            prefType = PrefType.Any;
            registerObserver();
        }

        public SharedPrefObserver(Context context, android.os.Handler handler, String name, String defValue) {
            super(handler);
            ctx = context;
            prefName = name;
            prefType = PrefType.String;
            prefDefValueString = defValue;
            registerObserver();
        }

        public SharedPrefObserver(Context context, android.os.Handler handler, String name) {
            super(handler);
            ctx = context;
            prefName = name;
            prefType = PrefType.StringSet;
            registerObserver();
        }

        public SharedPrefObserver(Context context, android.os.Handler handler, String name, int defValue) {
            super(handler);
            ctx = context;
            prefType = PrefType.Integer;
            prefName = name;
            prefDefValueInt = defValue;
            registerObserver();
        }

        public SharedPrefObserver(Context context, Handler handler, String name, boolean defValue) {
            super(handler);
            ctx = context;
            prefType = PrefType.Boolean;
            prefName = name;
            prefDefValueBool = defValue;
            registerObserver();
        }

        void registerObserver() {
            Uri uri = null;
            if (prefType == PrefType.String)
                uri = stringPrefToUri(prefName, prefDefValueString);
            else if (prefType == PrefType.StringSet)
                uri = stringSetPrefToUri(prefName);
            else if (prefType == PrefType.Integer)
                uri = intPrefToUri(prefName, prefDefValueInt);
            else if (prefType == PrefType.Boolean)
                uri = boolPrefToUri(prefName, prefDefValueBool);
            else if (prefType == PrefType.Any)
                uri = anyPrefToUri();
            if (uri != null)
                ctx.getContentResolver().registerContentObserver(uri, prefType == PrefType.Any, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (prefType == PrefType.Any)
                onChange(uri);
            else
                onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (selfChange) return;
            if (prefType == PrefType.String)
                onChange(prefName, prefDefValueString);
            else if (prefType == PrefType.StringSet)
                onChange(prefName);
            else if (prefType == PrefType.Integer)
                onChange(prefName, prefDefValueInt);
            else if (prefType == PrefType.Boolean)
                onChange(prefName, prefDefValueBool);
        }

        public void onChange(Uri uri) {
        }

        public void onChange(String name) {
        }

        public void onChange(String name, String defValue) {
        }

        public void onChange(String name, int defValue) {
        }

        public void onChange(String name, boolean defValue) {
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
                if (user != 0)
                    XposedHelpers.callMethod(context, "startActivityAsUser", intent, XposedHelpers.newInstance(UserHandle.class, user));
                else
                    context.startActivity(intent);
            } catch (Throwable t2) {
                logE(TAG, "openAppInfo" + t2);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getSharedStringSetPref(Context context, String name) {
        Uri uri = stringSetPrefToUri(name);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                Set<String> prefValue = new LinkedHashSet<>();
                while (cursor.moveToNext()) {
                    prefValue.add(cursor.getString(0));
                }
                cursor.close();
                return prefValue;
            } else {
                LogI("ContentResolver", "[" + name + "] Cursor fail: null");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        LinkedHashSet<String> empty = new LinkedHashSet<>();
        if (BaseHook.mPrefsMap.containsKey(name)) {
            return (Set<String>) BaseHook.mPrefsMap.getObject(name, empty);
        } else {
            return empty;
        }
    }

    public static int getSharedIntPref(Context context, String name, int defValue) {
        Uri uri = intPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue;
            } else LogI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (BaseHook.mPrefsMap.containsKey(name))
            return (int) BaseHook.mPrefsMap.getObject(name, defValue);
        else
            return defValue;
    }

    public static boolean getSharedBoolPref(Context context, String name, boolean defValue) {
        Uri uri = boolPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue == 1;
            } else LogI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            LogD("ContentResolver", t);
        }

        if (BaseHook.mPrefsMap.containsKey(name))
            return (boolean) BaseHook.mPrefsMap.getObject(name, false);
        else
            return defValue;
    }

    public static Uri stringPrefToUri(String name, String defValue) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/string/" + name + "/" + defValue);
    }

    public static Uri stringSetPrefToUri(String name) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/stringset/" + name);
    }

    public static Uri intPrefToUri(String name, int defValue) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/integer/" + name + "/" + defValue);
    }

    public static Uri boolPrefToUri(String name, boolean defValue) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/boolean/" + name + "/" + (defValue ? '1' : '0'));
    }

    public static Uri shortcutIconPrefToUri(String name) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/shortcut_icon/" + name);
    }

    public static Uri anyPrefToUri() {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/");
    }

    public static String getPackageVersionName(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            String versionName = (String) XposedHelpers.getObjectField(pkg, "mVersionName");
            //XposedLogUtils.logI("getPackageVersionName", lpparam.packageName + " versionName is " + versionName);
            return versionName;
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
            int versionCode = XposedHelpers.getIntField(pkg, "mVersionCode");
            //XposedLogUtils.logI("getPackageVersionCode", lpparam.packageName + " versionCode is " + versionCode);
            return versionCode;
        } catch (Throwable e) {
            //XposedLogUtils.logW("getPackageVersionCode", e);
            return -1;
        }
    }
}
