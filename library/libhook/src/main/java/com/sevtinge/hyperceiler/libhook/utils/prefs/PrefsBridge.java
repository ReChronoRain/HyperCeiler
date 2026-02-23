package com.sevtinge.hyperceiler.libhook.utils.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.github.libxposed.service.RemotePreferences;

public class PrefsBridge {
    public static final String PREFS_NAME = "hyperceiler_prefs";
    private static SharedPreferences mLocalPrefs;
    public static RemotePreferences mRemotePrefs; // LSPosed 远程句柄

    private static final HashSet<OnPrefsChangeListener> mObservers = new HashSet<>();

    public interface OnPrefsChangeListener {
        void onPrefChanged(String key);
    }

    // 初始化本地存储（主 App 进程使用）
    public static void init(Context context) {
        Context deContext = context.isDeviceProtectedStorage() ? context : context.createDeviceProtectedStorageContext();
        mLocalPrefs = deContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setRemotePrefs(RemotePreferences remotePrefs) {
        mRemotePrefs = remotePrefs;
    }

    public static RemotePreferences getRemotePrefs() {
        return mRemotePrefs;
    }

    public static SharedPreferences getLocalPrefs() {
        return mLocalPrefs;
    }

    private static SharedPreferences getImpl() {
        return (mRemotePrefs != null) ? mRemotePrefs : mLocalPrefs;
    }

    /**
     * 统一前缀处理逻辑，保持与你原 PrefsMap 一致
     */
    private static String wrapKey(String key) {
        if (key != null && !key.startsWith("prefs_key_")) {
            return "prefs_key_" + key;
        }
        return key;
    }

    // --- 兼容旧版 mPrefsMap 的 API ---
    // 这样你在模块里可以直接用 PrefsBridge.getBoolean("key")
    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return getImpl().getBoolean(wrapKey(key), defValue);
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String defValue) {
        return getImpl().getString(wrapKey(key), defValue);
    }

    public static int getStringAsInt(String key, int defValue) {
        String value = getString(key);
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defValue;
        }
    }

    public static int getInt(String key, int defValue) {
        return getImpl().getInt(wrapKey(key), defValue);
    }

    public static Set<String> getStringSet(String key) {
        Set<String> set = getImpl().getStringSet(wrapKey(key), null);
        return set == null ? new LinkedHashSet<>() : set;
    }

    public static void put(String key, Object value) {
        SharedPreferences.Editor editor = getImpl().edit();
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Set) editor.putStringSet(key, (Set<String>) value);
        editor.apply();
    }

    public static void putString(String key, String value) {
        SharedPreferences.Editor editor = getImpl().edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void putInt(String key, int value) {
        SharedPreferences.Editor editor = getImpl().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getImpl().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void putStringSet(String key, Set<String> values) {
        SharedPreferences.Editor editor = getImpl().edit();
        editor.putStringSet(key, values);
        editor.apply();
    }

    // --- 监听分发 ---
    public static void notifyChanged(String key) {
        for (OnPrefsChangeListener observer : mObservers) {
            observer.onPrefChanged(key);
        }
    }

    public static void registerObserver(OnPrefsChangeListener observer) {
        if (mObservers.isEmpty()) {
            getImpl().registerOnSharedPreferenceChangeListener((sp, key) -> notifyChanged(key));
        }
        mObservers.add(observer);
    }

    // --- 新增：获取全部 Key ---
    public static Map<String, ?> getAll() {
        return getImpl().getAll();
    }

    // --- 新增：删除单个 Key ---
    public static void remove(String key) {
        getImpl().edit().remove(key).apply();
    }

    // --- 核心重置逻辑：只负责清空 ---
    public static void clearAll() {
        getImpl().edit().clear().apply();
        // 发送 null 通知表示全量数据变更
        notifyChanged(null);
    }

    /**
     * 一键重置所有设置
     * @param context 上下文
     * @param xmlResId 对应的 preferences.xml 资源 ID（用于提取默认值）
     */
    public static void resetAll(Context context, int xmlResId) {
        // 1. 获取当前实际的操作句柄
        SharedPreferences impl = getImpl();

        // 2. 清空数据
        impl.edit().clear().apply();

        // 3. 重新加载 XML 定义的默认值
        // readAgain 设为 true 确保覆盖当前空状态
        PreferenceManager.setDefaultValues(context, PREFS_NAME, Context.MODE_PRIVATE, xmlResId, true);

        // 4. 发送一个特殊通知，触发所有 Observer 的全量刷新
        // 我们传入一个特殊的 key 或者 null，取决于你的业务逻辑
        notifyChanged("__all_reset__");
    }

}
