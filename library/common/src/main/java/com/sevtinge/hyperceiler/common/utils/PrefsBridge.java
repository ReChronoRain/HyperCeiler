package com.sevtinge.hyperceiler.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.utils.prefs.PrefType;
import com.sevtinge.hyperceiler.common.utils.prefs.PrefsChangeObserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PrefsBridge {

    private static final String TAG = "PrefsBridge";
    public static final String PREFS_NAME = "hyperceiler_prefs";
    private static final Set<String> sWarnedHookWrites = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, Object> sHookCache = new ConcurrentHashMap<>();

    // App 进程本地存储句柄
    private static SharedPreferences mPhysicalPrefs;
    private static Context mAppContext;
    // Hook 进程远程存储句柄（来自 LSPosed service）
    private static SharedPreferences mRemotePrefs;
    private static boolean isHookProcess = false;

    /**
     * App 进程初始化：在模块 Application.onCreate 中调用
     *
     * @param baseContext 建议传入普通的 Context，确保路径在 /data/user/0/ 下以便备份
     */
    public static void initForApp(@NonNull Context baseContext) {
        isHookProcess = false;
        Context protectedContext = getProtectedContext(baseContext);
        mAppContext = protectedContext;
        mPhysicalPrefs = protectedContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Context getProtectedContext(Context context) {
        return context.createDeviceProtectedStorageContext();
    }

    /**
     * Hook 进程初始化：在 Xposed 模块入口中调用
     * Hook 进程中的偏好设置仅支持读取，不应在该进程内执行写入。
     *
     * @param remote 传入 LSPosed 提供的远程 SharedPreferences 对象
     */
    public static void initForHook(@NonNull SharedPreferences remote) {
        isHookProcess = true;
        mRemotePrefs = remote;
    }

    /**
     * 绑定服务后的注入：在 Application.onServiceBind 中调用
     * 确保激活状态下，App 端的修改能推送到远程
     */
    public static void setRemotePrefs(SharedPreferences remote) {
        mRemotePrefs = remote;
        if (remote != null) {
            syncPhysicalToRemote(); // 激活瞬间全量同步
        }
    }

    /**
     * 获取原生 SharedPreferences 对象
     * <p>
     * 该方法仅用于直接读取底层 SharedPreferences。
     * 不要对返回值调用 edit() 进行写入，否则会绕过应用侧同步逻辑，
     * Hook 进程下还可能直接命中只读实现。
     * 如需持久化写入，请使用 putByApp/removeByApp/clearAllByApp。
     *
     * @return 根据当前进程环境返回物理句柄或远程句柄
     */
    public static SharedPreferences getSharedPreferences() {
        return getImpl();
    }

    public static boolean isHookProcess() {
        return isHookProcess;
    }

    /**
     * 仅更新 Hook 进程内的临时缓存，不会写入真实 prefs，也不会触发远端同步。
     * 适用于兼容旧版 mPrefsMap 的“刷新本地可见值”场景。
     */
    public static void putHookCache(String key, @Nullable Object value) {
        String rKey = wrap(key);
        if (value == null) {
            sHookCache.remove(rKey);
            return;
        }
        if (value instanceof Set<?> setValue) {
            LinkedHashSet<String> stringSet = new LinkedHashSet<>();
            for (Object item : setValue) {
                if (item instanceof String str) {
                    stringSet.add(str);
                }
            }
            sHookCache.put(rKey, stringSet);
            return;
        }
        sHookCache.put(rKey, value);
    }

    public static void removeHookCache(String key) {
        sHookCache.remove(wrap(key));
    }

    public static void clearHookCache() {
        sHookCache.clear();
    }

    private static String wrap(String key) {
        return (key != null && !key.startsWith("prefs_key_")) ? "prefs_key_" + key : key;
    }

    // --- 写入方法族 ---
    // 约定：所有写入都应由应用进程发起，再同步到远程。

    public static void putBoolean(String key, boolean val) {
        putByApp(key, val);
    }

    public static void putString(String key, String val) {
        putByApp(key, val);
    }

    public static void putInt(String key, int val) {
        putByApp(key, val);
    }

    public static void putLong(String key, long val) {
        putByApp(key, val);
    }

    public static void putFloat(String key, float val) {
        putByApp(key, val);
    }

    public static void putStringSet(String key, Set<String> vals) {
        putByApp(key, vals);
    }

    /**
     * 应用进程写入入口：先更新本地，再同步到远程。
     * Hook 进程调用会被忽略并输出警告。
     */
    public static void putByApp(String key, Object value) {
        String rKey = wrap(key);
        if (warnAndSkipIfHookWrite("put", rKey)) return;
        PrefType prefType = resolvePrefType(rKey, value);
        applyPut(mPhysicalPrefs, rKey, value, "physical");
        applyPut(mRemotePrefs, rKey, value, "remote");
        notifyPrefChanged(rKey, prefType);
    }

    /**
     * 兼容旧调用，语义等同于 {@link #putByApp(String, Object)}。
     */
    public static void put(String key, Object value) {
        putByApp(key, value);
    }

    /**
     * 移除特定配置项
     */
    public static void removeByApp(String key) {
        String rKey = wrap(key);
        if (warnAndSkipIfHookWrite("remove", rKey)) return;
        PrefType prefType = resolvePrefType(rKey, null);
        applyRemove(mPhysicalPrefs, rKey, "physical");
        applyRemove(mRemotePrefs, rKey, "remote");
        notifyPrefChanged(rKey, prefType);
    }

    /**
     * 兼容旧调用，语义等同于 {@link #removeByApp(String)}。
     */
    public static void remove(String key) {
        removeByApp(key);
    }

    // --- 读取方法 (自动根据当前环境切换读取源) ---

    /**
     * 读取布尔值
     */
    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * 读取布尔值
     */
    public static boolean getBoolean(String key, boolean def) {
        String rKey = wrap(key);
        Object cached = sHookCache.get(rKey);
        return cached instanceof Boolean ? (Boolean) cached : requireImpl().getBoolean(rKey, def);
    }

    /**
     * 读取字符串
     */
    public static String getString(String key, String def) {
        String rKey = wrap(key);
        Object cached = sHookCache.get(rKey);
        return cached instanceof String ? (String) cached : requireImpl().getString(rKey, def);
    }

    /**
     * 兼容方法：将 获取到的String转换为Int
     */
    public static int getStringAsInt(String key, int def) {
        String value = getString(key, String.valueOf(def));
        if (TextUtils.isEmpty(value)) {
            return def;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    /**
     * 读取整型，包含 String 转 Int 的鲁棒性处理
     */
    public static int getInt(String key, int def) {
        String rKey = wrap(key);
        Object cached = sHookCache.get(rKey);
        return cached instanceof Integer ? (Integer) cached : requireImpl().getInt(rKey, def);
    }

    /**
     * 读取长整型
     */
    public static long getLong(String key, long def) {
        String rKey = wrap(key);
        Object cached = sHookCache.get(rKey);
        return cached instanceof Long ? (Long) cached : requireImpl().getLong(rKey, def);
    }

    /**
     * 读取浮点型
     */
    public static float getFloat(String key, float def) {
        String rKey = wrap(key);
        Object cached = sHookCache.get(rKey);
        return cached instanceof Float ? (Float) cached : requireImpl().getFloat(rKey, def);
    }

    /**
     * 读取字符串集合
     */
    public static Set<String> getStringSet(String key) {
        return getStringSet(key, new LinkedHashSet<>());
    }

    /**
     * 读取字符串集合
     */
    @Nullable
    public static Set<String> getStringSet(String key, @Nullable Set<String> def) {
        String rKey = wrap(key);
        Object cached = sHookCache.get(rKey);
        if (cached instanceof Set<?> setValue) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (Object item : setValue) {
                if (item instanceof String str) {
                    result.add(str);
                }
            }
            return result;
        }
        Set<String> value = requireImpl().getStringSet(rKey, def);
        return value == null ? null : new LinkedHashSet<>(value);
    }

    /**
     * 获取全量配置 (注意：Hook 进程中大数据量调用可能导致启动变慢)
     */
    public static Map<String, ?> getAll() {
        // 强制从物理句柄读取，确保备份的是磁盘上的真实文件
        if (mPhysicalPrefs != null) {
            return mPhysicalPrefs.getAll();
        }
        // 如果物理句柄为空（极罕见），再尝试远程
        if (mRemotePrefs != null) {
            return mRemotePrefs.getAll();
        }
        return new HashMap<>();
    }


    // --- 内部维护工具 ---

    /**
     * 获取当前活动的配置句柄
     * 在 Hook 进程中，返回注入的远程句柄；在 App 进程，优先物理句柄
     */
    private static SharedPreferences getImpl() {
        if (isHookProcess && mRemotePrefs != null) return mRemotePrefs;
        return mPhysicalPrefs != null ? mPhysicalPrefs : mRemotePrefs;
    }

    private static SharedPreferences requireImpl() {
        SharedPreferences impl = getImpl();
        if (impl == null) {
            throw new IllegalStateException("PrefsBridge is not initialized. Call initForApp/initForHook first.");
        }
        return impl;
    }

    /**
     * 内部通用的写入映射
     */
    private static void performPut(SharedPreferences.Editor editor, String key, Object value) {
        if (value == null) {
            editor.remove(key);
            return;
        }
        if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set<?> setValue) {
            LinkedHashSet<String> stringSet = new LinkedHashSet<>();
            for (Object item : setValue) {
                if (item instanceof String str) {
                    stringSet.add(str);
                }
            }
            editor.putStringSet(key, stringSet);
        }
    }

    /**
     * 将物理数据同步到远程
     */
    private static void syncPhysicalToRemote() {
        if (mPhysicalPrefs == null || mRemotePrefs == null) return;
        Map<String, ?> all = mPhysicalPrefs.getAll();
        try {
            SharedPreferences.Editor remoteEdit = mRemotePrefs.edit();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                performPut(remoteEdit, entry.getKey(), entry.getValue());
            }
            remoteEdit.apply();
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "Remote SharedPreferences is read-only while syncing from app process.", e);
        }
    }

    /**
     * 一键重置：清空所有本地与远程数据
     */
    public static void clearAllByApp() {
        if (warnAndSkipIfHookWrite("clearAll", PREFS_NAME)) return;
        Map<String, ?> allEntries = new HashMap<>(getAll());
        applyClear(mPhysicalPrefs, "physical");
        applyClear(mRemotePrefs, "remote");
        clearHookCache();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            notifyPrefChanged(entry.getKey(), resolvePrefType(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * 兼容旧调用，语义等同于 {@link #clearAllByApp()}。
     */
    public static void clearAll() {
        clearAllByApp();
    }

    private static boolean warnAndSkipIfHookWrite(String operation, String key) {
        if (!isHookProcess) return false;
        String warnKey = operation + ":" + key;
        if (sWarnedHookWrites.add(warnKey)) {
            Log.w(TAG, "Ignored prefs " + operation + " for " + key
                + " in hook process. Hook processes are read-only; perform writes from the app process instead.");
        }
        return true;
    }

    private static void applyPut(@Nullable SharedPreferences prefs, String key, Object value, String source) {
        if (prefs == null) return;
        try {
            SharedPreferences.Editor editor = prefs.edit();
            performPut(editor, key, value);
            editor.apply();
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "Failed to put " + key + " to " + source + " prefs because the implementation is read-only.", e);
        }
    }

    private static void applyRemove(@Nullable SharedPreferences prefs, String key, String source) {
        if (prefs == null) return;
        try {
            prefs.edit().remove(key).apply();
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "Failed to remove " + key + " from " + source + " prefs because the implementation is read-only.", e);
        }
    }

    private static void applyClear(@Nullable SharedPreferences prefs, String source) {
        if (prefs == null) return;
        try {
            prefs.edit().clear().apply();
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "Failed to clear " + source + " prefs because the implementation is read-only.", e);
        }
    }

    @Nullable
    private static PrefType resolvePrefType(String key, @Nullable Object value) {
        Object target = value;
        if (target == null && mPhysicalPrefs != null) {
            target = mPhysicalPrefs.getAll().get(key);
        }
        if (target instanceof String) return PrefType.String;
        if (target instanceof Boolean) return PrefType.Boolean;
        if (target instanceof Integer) return PrefType.Integer;
        if (target instanceof Set<?>) return PrefType.StringSet;
        return null;
    }

    private static void notifyPrefChanged(String key, @Nullable PrefType prefType) {
        if (mAppContext == null || key == null) {
            return;
        }
        try {
            if (prefType != null) {
                mAppContext.getContentResolver().notifyChange(PrefsChangeObserver.PrefToUri.prefToUri(prefType, key), null);
                mAppContext.getContentResolver().notifyChange(PrefsChangeObserver.PrefToUri.anyPrefToUri(prefType, key), null);
                return;
            }

            for (PrefType type : new PrefType[]{PrefType.String, PrefType.StringSet, PrefType.Integer, PrefType.Boolean}) {
                mAppContext.getContentResolver().notifyChange(PrefsChangeObserver.PrefToUri.prefToUri(type, key), null);
                mAppContext.getContentResolver().notifyChange(PrefsChangeObserver.PrefToUri.anyPrefToUri(type, key), null);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to notify pref change for " + key, t);
        }
    }
}
