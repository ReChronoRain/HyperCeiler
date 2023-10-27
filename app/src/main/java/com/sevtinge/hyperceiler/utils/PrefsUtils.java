package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

public class PrefsUtils {

    public static SharedPreferences mSharedPreferences = null;

    public static String mPrefsPathCurrent = null;
    public static String mPrefsFileCurrent = null;
    public static String mPrefsName = "hyperceiler_prefs";
    public static String mPrefsPath = "/data/user_de/0/" + Helpers.mAppModulePkg + "/shared_prefs";
    public static String mPrefsFile = mPrefsPath + "/" + mPrefsName + ".xml";


    public static SharedPreferences getSharedPrefs(Context context, boolean multiProcess) {
        context = Helpers.getProtectedContext(context);
        try {
            return context.getSharedPreferences(mPrefsName, multiProcess ? Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE : Context.MODE_WORLD_READABLE);
        } catch (Throwable t) {
            return context.getSharedPreferences(mPrefsName, multiProcess ? Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE : Context.MODE_PRIVATE);
        }
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        return getSharedPrefs(context, false);
    }


    public static String getSharedPrefsPath() {
        if (mPrefsPathCurrent == null) try {
            Field mFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            mFile.setAccessible(true);
            mPrefsPathCurrent = ((File) mFile.get(mSharedPreferences)).getParentFile().getAbsolutePath();
            return mPrefsPathCurrent;
        } catch (Throwable t) {
            System.out.print("Test" + t);
            return mPrefsPath;
        }
        else return mPrefsPathCurrent;
    }

    public static String getSharedPrefsFile() {
        if (mPrefsFileCurrent == null) try {
            Field fFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            fFile.setAccessible(true);
            mPrefsFileCurrent = ((File) fFile.get(mSharedPreferences)).getAbsolutePath();
            System.out.println("Test: mPrefsFileCurrent");
            return mPrefsFileCurrent;
        } catch (Throwable t) {
            System.out.println("Test: mPrefsFile" + t);
            return mPrefsFile;
        }
        else
            System.out.println("Test: mPrefsFileCurrent2");
        return mPrefsFileCurrent;
    }


    public static boolean contains(String key) {
        return mSharedPreferences.contains(key);
    }

    public static SharedPreferences.Editor editor() {
        return mSharedPreferences.edit();
    }


    public static String getSharedStringPrefs(Context context, String name, String defValue) {
        Uri uri = stringPrefsToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String prefValue = cursor.getString(0);
                cursor.close();
                return prefValue;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (XposedInit.mPrefsMap.containsKey(name))
            return (String) XposedInit.mPrefsMap.getObject(name, defValue);
        else return defValue;
    }

    public static Set<String> getSharedStringSetPrefs(Context context, String name) {
        Uri uri = stringSetPrefsToUri(name);
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
                XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: null");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        LinkedHashSet<String> empty = new LinkedHashSet<>();
        if (XposedInit.mPrefsMap.containsKey(name)) {
            return (Set<String>) XposedInit.mPrefsMap.getObject(name, empty);
        } else {
            return empty;
        }
    }


    public static int getSharedIntPrefs(Context context, String name, int defValue) {
        Uri uri = intPrefsToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (XposedInit.mPrefsMap.containsKey(name))
            return (int) XposedInit.mPrefsMap.getObject(name, defValue);
        else return defValue;
    }


    public static boolean getSharedBoolPrefs(Context context, String name, boolean defValue) {
        Uri uri = boolPrefsToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue == 1;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (XposedInit.mPrefsMap.containsKey(name))
            return (boolean) XposedInit.mPrefsMap.getObject(name, false);
        else
            return defValue;
    }


    public static Uri stringPrefsToUri(String name, String defValue) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/string/" + name + "/" + defValue);
    }

    public static Uri stringSetPrefsToUri(String name) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/stringset/" + name);
    }

    public static Uri intPrefsToUri(String name, int defValue) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/integer/" + name + "/" + defValue);
    }

    public static Uri boolPrefsToUri(String name, boolean defValue) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/boolean/" + name + "/" + (defValue ? '1' : '0'));
    }

    public static Uri shortcutIconPrefsToUri(String name) {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/shortcut_icon/" + name);
    }

    public static Uri anyPrefsToUri() {
        return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/");
    }


    public static class SharedPrefsObserver extends ContentObserver {

        enum PrefType {
            Any, String, StringSet, Integer, Boolean
        }

        PrefType prefType;
        Context ctx;
        String mPrefsName;
        String mPrefsDefValueString;
        int mPrefsDefValueInt;
        boolean mPrefsDefValueBool;

        public SharedPrefsObserver(Context context, Handler handler) {
            super(handler);
            ctx = context;
            prefType = PrefType.Any;
            registerObserver();
        }

        public SharedPrefsObserver(Context context, Handler handler, String name, String defValue) {
            super(handler);
            ctx = context;
            mPrefsName = name;
            prefType = PrefType.String;
            mPrefsDefValueString = defValue;
            registerObserver();
        }

        public SharedPrefsObserver(Context context, Handler handler, String name) {
            super(handler);
            ctx = context;
            mPrefsName = name;
            prefType = PrefType.StringSet;
            registerObserver();
        }

        public SharedPrefsObserver(Context context, Handler handler, String name, int defValue) {
            super(handler);
            ctx = context;
            prefType = PrefType.Integer;
            mPrefsName = name;
            mPrefsDefValueInt = defValue;
            registerObserver();
        }

        @SuppressLint("SuspiciousIndentation")
        public SharedPrefsObserver(Context context, Handler handler, String name, boolean defValue) {
            super(handler);
            ctx = context;
            prefType = PrefType.Boolean;
            mPrefsName = name;
            mPrefsDefValueBool = defValue;
            registerObserver();
        }

        void registerObserver() {
            Uri uri = null;
            if (prefType == PrefType.String)
                uri = stringPrefsToUri(mPrefsName, mPrefsDefValueString);
            else if (prefType == PrefType.StringSet)
                uri = stringSetPrefsToUri(mPrefsName);
            else if (prefType == PrefType.Integer)
                uri = intPrefsToUri(mPrefsName, mPrefsDefValueInt);
            else if (prefType == PrefType.Boolean)
                uri = boolPrefsToUri(mPrefsName, mPrefsDefValueBool);
            else if (prefType == PrefType.Any)
                uri = anyPrefsToUri();
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
                onChange(mPrefsName, mPrefsDefValueString);
            else if (prefType == PrefType.StringSet)
                onChange(mPrefsName);
            else if (prefType == PrefType.Integer)
                onChange(mPrefsName, mPrefsDefValueInt);
            else if (prefType == PrefType.Boolean)
                onChange(mPrefsName, mPrefsDefValueBool);
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
}
