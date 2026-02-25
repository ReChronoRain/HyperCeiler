package com.sevtinge.hyperceiler.libhook.utils.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class PrefsBridge {

    private static final String TAG = "PrefsBridge";
    public static final String PREFS_NAME = "hyperceiler_prefs";

    private static SharedPreferences mPhysicalPrefs; // 物理文件句柄 (用于 App 进程强制落盘)
    private static SharedPreferences mRemotePrefs;   // 远程代理句柄 (用于 Hook 进程数据同步)
    private static boolean isHookProcess = false;

    /**
     * App 进程初始化：在模块 Application.onCreate 中调用
     *
     * @param baseContext 建议传入普通的 Context，确保路径在 /data/user/0/ 下以便备份
     */
    public static void initForApp(@NonNull Context baseContext) {
        isHookProcess = false;
        mPhysicalPrefs = baseContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Hook 进程初始化：在 Xposed 模块入口中调用
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
     *
     * @return 根据当前进程环境返回物理句柄或远程句柄
     */
    public static SharedPreferences getSharedPreferences() {
        return getImpl();
    }

    /**
     * 格式化 Key 名称，确保所有存取都带上前缀
     */
    private static String wrap(String key) {
        return (key != null && !key.startsWith("prefs_key_")) ? "prefs_key_" + key : key;
    }

    // --- 写入方法族 (支持双轨同步与物理强制落盘) ---

    public static void putBoolean(String key, boolean val) {
        put(key, val);
    }

    public static void putString(String key, String val) {
        put(key, val);
    }

    public static void putInt(String key, int val) {
        put(key, val);
    }

    public static void putLong(String key, long val) {
        put(key, val);
    }

    public static void putFloat(String key, float val) {
        put(key, val);
    }

    public static void putStringSet(String key, Set<String> vals) {
        put(key, vals);
    }

    /**
     * 核心写入逻辑
     * 在 App 进程中，强制使用 commit() 确保物理 XML 文件有内容且可备份
     * 在远程端使用 apply() 确保跨进程通信不阻塞 UI
     */
    public static void put(String key, Object value) {
        String rKey = wrap(key);
        // 1. 物理落盘 (解决备份文件为空的问题)
        if (mPhysicalPrefs != null) {
            SharedPreferences.Editor editor = mPhysicalPrefs.edit();
            performPut(editor, rKey, value);
            editor.commit();
        }
        // 2. 远程同步 (解决 Hook 进程实时生效的问题)
        if (mRemotePrefs != null) {
            SharedPreferences.Editor editor = mRemotePrefs.edit();
            performPut(editor, rKey, value);
            editor.apply();
        }
    }

    /**
     * 移除特定配置项
     */
    public static void remove(String key) {
        String rKey = wrap(key);
        if (mPhysicalPrefs != null) mPhysicalPrefs.edit().remove(rKey).commit();
        if (mRemotePrefs != null) mRemotePrefs.edit().remove(rKey).apply();
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
        return getImpl().getBoolean(wrap(key), def);
    }

    /**
     * 读取字符串
     */
    public static String getString(String key, String def) {
        return getImpl().getString(wrap(key), def);
    }

    /**
     * 兼容方法：将 获取到的String转换为Int
     */
    public static int getStringAsInt(String key, int def) {
        String value = getString(key, String.valueOf(def));
        if (TextUtils.isEmpty(value)) {
            return def;
        }
        return Integer.parseInt(value);
    }

    /**
     * 读取整型，包含 String 转 Int 的鲁棒性处理
     */
    public static int getInt(String key, int def) {
        return getImpl().getInt(wrap(key), def);
    }

    /**
     * 读取长整型
     */
    public static long getLong(String key, long def) {
        return getImpl().getLong(wrap(key), def);
    }

    /**
     * 读取浮点型
     */
    public static float getFloat(String key, float def) {
        return getImpl().getFloat(wrap(key), def);
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
        return getImpl().getStringSet(wrap(key), def);
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

    /**
     * 内部通用的写入映射
     */
    private static void performPut(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set) editor.putStringSet(key, (Set<String>) value);
    }

    /**
     * 将物理数据同步到远程
     */
    private static void syncPhysicalToRemote() {
        if (mPhysicalPrefs == null || mRemotePrefs == null) return;
        Map<String, ?> all = mPhysicalPrefs.getAll();
        SharedPreferences.Editor remoteEdit = mRemotePrefs.edit();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            performPut(remoteEdit, entry.getKey(), entry.getValue());
        }
        remoteEdit.apply();
    }

    /**
     * 一键重置：清空所有本地与远程数据
     */
    public static void clearAll() {
        if (mPhysicalPrefs != null) mPhysicalPrefs.edit().clear().commit();
        if (mRemotePrefs != null) mRemotePrefs.edit().clear().apply();
    }
}
