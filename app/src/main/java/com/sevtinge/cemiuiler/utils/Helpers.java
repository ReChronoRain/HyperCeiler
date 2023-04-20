package com.sevtinge.cemiuiler.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;
import android.widget.TextView;
import com.sevtinge.cemiuiler.BuildConfig;

import com.sevtinge.cemiuiler.R;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.provider.SharedPrefsProvider;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import moralnorm.internal.utils.ReflectUtils;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.PreferenceScreen;

public class Helpers {

    private static final String TAG = "Helpers";

    public static Context mModuleContext = null;

    public static boolean isModuleActive = false;
    public static int XposedVersion = 0;

    public static String mAppModulePkg = BuildConfig.APPLICATION_ID;

    public static ArrayList<ModData> allModsList = new ArrayList<ModData>();

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    public static final String MIUIZER_NS = "http://schemas.android.com/apk/res-auto";

    public static final int REQUEST_PERMISSIONS_BACKUP = 1;
    public static final int REQUEST_PERMISSIONS_RESTORE = 2;


    public static LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024) / 2) {
        @Override
        protected int sizeOf(String key, Bitmap icon) {
            if (icon != null)
                return icon.getAllocationByteCount() / 1024;
            else
                return 130 * 130 * 4 / 1024;
        }
    };

    public static synchronized Context getProtectedContext(Context context) {
        return getProtectedContext(context, null);
    }

    public static synchronized Context getProtectedContext(Context context, Configuration config) {
        try {
            Context mContext = context.isDeviceProtectedStorage() ? context : context.createDeviceProtectedStorageContext();
            return config == null ? mContext : mContext.createConfigurationContext(config);
        } catch (Throwable t) {
            return context;
        }
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

    public static Object getStaticObjectFieldSilently(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            return null;
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


    public static void getAllMods(Context context, boolean force) {
        if (force) allModsList.clear();
        else if (allModsList.size() > 0) return;
        parsePrefXml(context, R.xml.home);
        parsePrefXml(context, R.xml.security_center);
        parsePrefXml(context, R.xml.various);
    }

    private static String getModTitle(Resources res, String title) {
        if (title == null) return null;
        int titleResId = Integer.parseInt(title.substring(1));
        if (titleResId <= 0) return null;
        return res.getString(titleResId);
    }

    @SuppressLint("NonConstantResourceId")
    private static void parsePrefXml(Context context, int xmlResId) {
        Resources res = context.getResources();
        String lastPrefSub = null;
        String lastPrefSubTitle = null;
        String lastPrefSubSubTitle = null;
        int catResId = 0;
        ModData.ModCat catPrefKey = null;

        switch (xmlResId) {
            case R.xml.home:
                catResId = R.string.home;
                catPrefKey = ModData.ModCat.prefs_key_home;
                break;
            case R.xml.security_center:
                catResId = R.string.security;
                catPrefKey = ModData.ModCat.prefs_key_security_center;
                break;
            case R.xml.various:
                catResId = R.string.various;
                catPrefKey = ModData.ModCat.prefs_key_various;
                break;
        }

        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            int order = 0;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && !PreferenceScreen.class.getSimpleName().equals(xml.getName()))
                    try {
                        if (xml.getName().equals(PreferenceCategory.class.getSimpleName()) || xml.getName().equals(PreferenceCategory.class.getCanonicalName())) {
                            if (xml.getAttributeValue(ANDROID_NS, "key") != null) {
                                lastPrefSub = xml.getAttributeValue(ANDROID_NS, "key");
                                lastPrefSubTitle = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"));
                                lastPrefSubSubTitle = null;
                                order = 1;
                            } else {
                                lastPrefSubSubTitle = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"));
                                order++;
                            }
                            eventType = xml.next();
                            continue;
                        }

                        ModData modData = new ModData();
                        boolean isChild = xml.getAttributeBooleanValue(MIUIZER_NS, "child", false);
                        if (!isChild) {
                            modData.title = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"));
                            if (modData.title != null) {
                                modData.breadcrumbs = res.getString(catResId) + (lastPrefSubTitle == null ? "" : ("/" + lastPrefSubTitle + (lastPrefSubSubTitle == null ? "" : "/" + lastPrefSubSubTitle)));
                                modData.key = xml.getAttributeValue(ANDROID_NS, "key");
                                modData.cat = catPrefKey;
                                modData.sub = lastPrefSub;
                                modData.order = order;
                                allModsList.add(modData);
                                //Log.e("miuizer", modData.key + " = " + modData.order);
                            }
                        }
                        order++;
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                eventType = xml.next();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
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


    /*Permissions权限*/
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


    public static void log(String line) {
        XposedBridge.log("Cemiuiler: " + line);
    }

    public static void log(Throwable t) {
        XposedBridge.log("Cemiuiler: " + t);
    }

    public static void log(String mod, String line) {
        XposedBridge.log("Cemiuiler: " + mod + " " + line);
    }

    private static String getCallerMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : stackTrace)
            if (el != null && el.getClassName().startsWith(mAppModulePkg + ".module")) return el.getMethodName();
        return stackTrace[4].getMethodName();
    }

    public static void hookMethod(Method method, MethodHook callback) {
        try {
            XposedBridge.hookMethod(method, callback);
        } catch (Throwable t) {
            log(getCallerMethod(), "Failed to hook " + method.getName() + " method");
        }
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
            Log.d(TAG, "MiuiMultiWindowUtils getActivityOptions error", e);
            activityOptions = null;
        }

        if (activityOptions != null) {
            return activityOptions;
        }


        ActivityOptions makeBasic = ActivityOptions.makeBasic();
        ReflectUtils.callObjectMethod("android.app.ActivityOptions", "setLaunchWindowingMode", new Class[]{int.class}, new Object[]{5});
        Rect rect = (Rect) ReflectUtils.callObjectMethod("android.util.MiuiMultiWindowUtils", "getFreeformRect", new Class[]{Context.class}, new Object[]{context});
        makeBasic.setLaunchBounds(rect);
        return makeBasic;
    }



    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            log(getCallerMethod(), "Success to hook " + methodName + " method in " + className);
        } catch (Throwable t) {
            log(getCallerMethod(), "Failed to hook " + methodName + " method in " + className);
        }
    }

    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            log(getCallerMethod(), "Failed to hook " + methodName + " method in " + clazz.getCanonicalName());
        }
    }

    public static boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            log(getCallerMethod(), "Success to hook " + methodName + " method in " + className);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            log(getCallerMethod(), "Success to hook " + methodName + " method in " + clazz.getCanonicalName());
            return true;
        } catch (Throwable t) {
            log(getCallerMethod(), "Failed to hook " + methodName + " method in " + clazz.getCanonicalName() + " Error: " + t);
            return false;
        }
    }

    public static void findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
            log(getCallerMethod(), "Success to hook constructor in " + className);
        } catch (Throwable t) {
            log(getCallerMethod(), "Failed to hook constructor in " + className + " Error: " + t);
        }
    }

    public static void findAndHookConstructor(Class<?> hookClass, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookConstructor(hookClass, parameterTypesAndCallback);
            log(getCallerMethod(), "Success to hook constructor in " + hookClass);
        } catch (Throwable t) {
            log(getCallerMethod(), "Failed to hook constructor in " + hookClass + " Error: " + t);
        }
    }


    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedBridge.hookAllMethods(hookClass, methodName, callback).size() == 0) ;
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        try {
            log(getCallerMethod(), "Success to hook " + methodName + " method in " + hookClass.getCanonicalName());
            if (XposedBridge.hookAllMethods(hookClass, methodName, callback).size() == 0)
                log(getCallerMethod(), "Failed to hook " + methodName + " method in " + hookClass.getCanonicalName());
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            log(getCallerMethod(), "Success to hook " + className + " constructor");
            if (hookClass == null || XposedBridge.hookAllConstructors(hookClass, callback).size() == 0)
                log(getCallerMethod(), "Failed to hook " + className + " constructor");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void hookAllConstructors(Class<?> hookClass, MethodHook callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                log(getCallerMethod(), "Success to hook " + hookClass.getPackageName() + "/" + hookClass.getCanonicalName() + " constructor");
            }
            if (XposedBridge.hookAllConstructors(hookClass, callback).size() == 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    log(getCallerMethod(), "Failed to hook " + hookClass.getPackageName() + "/" + hookClass.getCanonicalName() + " constructor");
                }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }


    public static boolean hookAllMethodsSilently(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            return hookClass != null && XposedBridge.hookAllMethods(hookClass, methodName, callback).size() > 0;
        } catch (Throwable t) {
            return false;
        }
    }


    public static boolean hookAllMethodsSilently(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        try {
            return hookClass != null && XposedBridge.hookAllMethods(hookClass, methodName, callback).size() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getSharedStringSetPref(Context context, String name) {
        Uri uri = stringSetPrefToUri(name);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                Set<String> prefValue = new LinkedHashSet<String>();
                while (cursor.moveToNext()) prefValue.add(cursor.getString(0));
                cursor.close();
                return prefValue;
            } else log("ContentResolver", "[" + name + "] Cursor fail: null");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        LinkedHashSet<String> empty = new LinkedHashSet<String>();
        if (BaseHook.mPrefsMap.containsKey(name))
            return (Set<String>) BaseHook.mPrefsMap.getObject(name, empty);
        else
            return empty;
    }

    public static int getSharedIntPref(Context context, String name, int defValue) {
        Uri uri = intPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue;
            } else log("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (BaseHook.mPrefsMap.containsKey(name))
            return (int) BaseHook.mPrefsMap.getObject(name, defValue);
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

    public static boolean isAndroidVersionTiramisu() {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.R: // 30
                return false;
            case Build.VERSION_CODES.S_V2: // 32
                return false;
            case Build.VERSION_CODES.S: // 31
                return false;
            case Build.VERSION_CODES.TIRAMISU: // 33
                return true;
            default:
                LogUtils.log(" Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                break;
        }
        return false;
    }


    public static class MethodHook extends XC_MethodHook {


        protected void before(MethodHookParam param) throws Throwable {
        }

        protected void after(MethodHookParam param) throws Throwable {
        }

        public MethodHook() {
            super();
        }

        public MethodHook(int priority) {
            super(priority);
        }

        @Override
        public final void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.before(param);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        @Override
        public final void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                this.after(param);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    public static String getPackageVersionName(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            String versionName = (String) XposedHelpers.getObjectField(pkg, "mVersionName");
            XposedBridge.log("Cemiuiler: " + lpparam + " versionName is " + versionName);
            return versionName;
        } catch (Throwable e) {
            XposedBridge.log("Cemiuiler: Unknown Version.");
            XposedBridge.log(e);
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
            XposedBridge.log("Cemiuiler: " + lpparam + " versionCode is " + versionCode);
            return versionCode;
        } catch (Throwable e) {
            XposedBridge.log("Cemiuiler: Unknown Version.");
            XposedBridge.log(e);
            return -1;
        }
    }

}
