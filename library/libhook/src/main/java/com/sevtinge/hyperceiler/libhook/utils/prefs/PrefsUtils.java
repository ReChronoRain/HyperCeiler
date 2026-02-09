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
package com.sevtinge.hyperceiler.libhook.utils.prefs;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getProtectedContext;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.RemotePreferences;

public class PrefsUtils {

    private static final String TAG = "PrefsUtils";

    public static SharedPreferences mSharedPreferences = null;
    public static volatile RemotePreferences remotePrefs = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();

    private static SharedPreferences.OnSharedPreferenceChangeListener sPrefsChangeListener = null;
    private static volatile boolean sListenerRegistered = false;

    public static String mPrefsPathCurrent = null;
    public static String mPrefsFileCurrent = null;
    public static String mPrefsName = "hyperceiler_prefs";
    public static String mPrefsPath = "/data/user_de/0/" + ProjectApi.mAppModulePkg + "/shared_prefs";
    public static String mPrefsFile = mPrefsPath + "/" + mPrefsName + ".xml";

    private static final CopyOnWriteArraySet<PreferenceObserver> prefObservers = new CopyOnWriteArraySet<>();

    public interface PreferenceObserver {
        void onChange(String key);
    }

    public static void observePreferenceChange(PreferenceObserver prefObserver) {
        prefObservers.add(prefObserver);
    }

    public static void handlePreferenceChanged(@Nullable String key) {
        for (PreferenceObserver prefObserver : prefObservers) {
            prefObserver.onChange(key);
        }
    }

    public static String getSharedPrefsPath() {
        if (mPrefsPathCurrent == null) try {
            Field mFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            mFile.setAccessible(true);
            mPrefsPathCurrent = ((File) mFile.get(mSharedPreferences)).getParentFile().getAbsolutePath();
            return mPrefsPathCurrent;
        } catch (Throwable t) {
            return mPrefsPath;
        }
        else return mPrefsPathCurrent;
    }

    public static String getSharedPrefsFile() {
        if (mPrefsFileCurrent == null) try {
            Field fFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            fFile.setAccessible(true);
            mPrefsFileCurrent = ((File) fFile.get(mSharedPreferences)).getAbsolutePath();
            return mPrefsFileCurrent;
        } catch (Throwable t) {
            return mPrefsFile;
        }
        else return mPrefsFileCurrent;
    }

    public static boolean contains(String key) {
        if (mSharedPreferences == null) return false;
        return mSharedPreferences.contains(key);
    }

    public static void putString(String key, String defValue) {
        if (mSharedPreferences == null) return;
        mSharedPreferences.edit().putString(key, defValue).apply();
    }

    public static void putInt(String key, int defValue) {
        mSharedPreferences.edit().putInt(key, defValue).apply();
    }

    public static void putStringSet(String key, Set<String> values) {
        mSharedPreferences.edit().putStringSet(key, values).apply();
    }

    /**
     * 获取 SharedPreferences Editor
     */
    public static SharedPreferences.Editor editor() {
        if (mSharedPreferences != null) {
            return mSharedPreferences.edit();
        }
        throw new IllegalStateException("SharedPreferences not initialized");
    }

    /**
     * 获取 SharedPreferences
     */
    public static SharedPreferences getSharedPrefs(Context context, boolean protectedStorage) {
        if (protectedStorage) context = getProtectedContext(context);
        return context.getSharedPreferences(mPrefsName, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        mSharedPreferences = getSharedPrefs(context);
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        return getSharedPrefs(context, false);
    }

    /**
     * 获取 Boolean 类型的偏好设置
     */
    public static boolean getSharedBoolPrefs(Context context, String name, boolean defValue) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            return prefs.getBoolean(name, defValue);
        } catch (Throwable t) {
            if (mPrefsMap.containsKey(name))
                return (boolean) mPrefsMap.getObject(name, defValue);
            else
                return defValue;
        }
    }

    /**
     * 获取 String 类型的偏好设置
     */
    public static String getSharedStringPrefs(Context context, String name, String defValue) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            return prefs.getString(name, defValue);
        } catch (Throwable t) {
            if (mPrefsMap.containsKey(name))
                return (String) mPrefsMap.getObject(name, defValue);
            else
                return defValue;
        }
    }

    /**
     * 获取 Int 类型的偏好设置
     */
    public static int getSharedIntPrefs(Context context, String name, int defValue) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            return prefs.getInt(name, defValue);
        } catch (Throwable t) {
            if (mPrefsMap.containsKey(name))
                return (int) mPrefsMap.getObject(name, defValue);
            else
                return defValue;
        }
    }

    /**
     * 获取 StringSet 类型的偏好设置
     */
    public static Set<String> getSharedStringSetPrefs(Context context, String name) {
        try {
            SharedPreferences prefs = getSharedPrefs(context);
            Set<String> result = prefs.getStringSet(name, null);
            if (result != null) {
                return result;
            }
        } catch (Throwable ignored) {
        }

        LinkedHashSet<String> empty = new LinkedHashSet<>();
        if (mPrefsMap.containsKey(name)) {
            return (Set<String>) mPrefsMap.getObject(name, empty);
        } else {
            return empty;
        }
    }

    /**
     * 将单个值按类型写入 RemotePreferences.Editor
     *
     * @return 对应的 ContentProvider path 前缀（如 "boolean/"），null 表示 val 为 null（需 remove）
     */
    @SuppressWarnings("unchecked")
    private static String putValueToEditor(RemotePreferences.Editor editor, String key, Object val) {
        switch (val) {
            case null -> {
                editor.remove(key);
                return null;
            }
            case Boolean b -> {
                editor.putBoolean(key, b);
                return "boolean/";
            }
            case Float v -> {
                editor.putFloat(key, v);
                return "float/";
            }
            case Integer i -> {
                editor.putInt(key, i);
                return "integer/";
            }
            case Long l -> {
                editor.putLong(key, l);
                return "long/";
            }
            case String s -> {
                editor.putString(key, s);
                return "string/";
            }
            case Set<?> ignored -> {
                editor.putStringSet(key, (Set<String>) val);
                return "stringset/";
            }
            default -> {
            }
        }
        return null;
    }

    /**
     * 将 SharedPreferences 中的所有配置全量同步到 RemotePreferences。
     * 应在 onServiceBind 获取到 remotePrefs 后立即调用，确保 Hook 端能读到完整配置。
     * 使用 commit() 同步提交，保证写入完成后再返回。
     */
    public static void syncAllToRemotePrefs() {
        final RemotePreferences prefs = remotePrefs; // 局部快照，避免竞态
        if (prefs == null || mSharedPreferences == null) return;

        try {
            Map<String, ?> allEntries = mSharedPreferences.getAll();
            if (allEntries == null || allEntries.isEmpty()) return;

            RemotePreferences.Editor editor = prefs.edit();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                putValueToEditor(editor, entry.getKey(), entry.getValue());
            }
            editor.commit(); // 同步提交，确保全量数据写入完成
        } catch (Throwable t) {
            AndroidLog.e(TAG, "Failed to sync all prefs to RemotePreferences", t);
        }
    }

    public static void registerOnSharedPreferenceChangeListener(Context context) {
        if (sListenerRegistered || mSharedPreferences == null) return;
        synchronized (PrefsUtils.class) {
            if (sListenerRegistered) return;

            HashSet<String> ignoreKeys = new HashSet<>();

            sPrefsChangeListener = (sharedPreferences, key) -> {
                // 局部快照，解决 TOCTOU 竞态：在整个回调期间使用同一个引用
                final RemotePreferences prefs = remotePrefs;
                if (prefs == null) return;

                AppsTool.requestBackup(context);

                if (key == null) {
                    try {
                        RemotePreferences.Editor prefEdit = prefs.edit();
                        for (String remoteKey : prefs.getAll().keySet()) {
                            prefEdit.remove(remoteKey);
                        }
                        prefEdit.apply();
                    } catch (Throwable t) {
                        AndroidLog.e(TAG, "Failed to clear RemotePreferences", t);
                    }
                    return;
                }

                if (ignoreKeys.contains(key)) return;

                try {
                    Object val = sharedPreferences.getAll().get(key);
                    RemotePreferences.Editor prefEdit = prefs.edit();
                    String path = putValueToEditor(prefEdit, key, val);

                    // 先提交数据，再发送通知，确保 Observer 读取时数据已就绪
                    prefEdit.apply();

                    if (path == null) path = "";
                    ContentResolver resolver = context.getContentResolver();
                    resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/" + path + key), null);
                    if (!path.isEmpty())
                        resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/" + path + key), null);
                } catch (Throwable t) {
                    AndroidLog.e(TAG, "Failed to sync pref key: " + key, t);
                }
            };

            mSharedPreferences.registerOnSharedPreferenceChangeListener(sPrefsChangeListener);
            sListenerRegistered = true;
        }
    }
}
