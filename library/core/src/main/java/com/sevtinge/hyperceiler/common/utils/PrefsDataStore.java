package com.sevtinge.hyperceiler.common.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.util.Set;

/**
 * 自定义 Preference 存储拦截器
 * 用于 PreferenceFragmentCompat，将 UI 修改自动同步到 PrefsBridge
 */
public class PrefsDataStore extends PreferenceDataStore {

    @Override
    public void putString(@NonNull String key, @Nullable String value) {
        PrefsBridge.putString(key, value);
    }

    @Override
    public void putStringSet(@NonNull String key, @Nullable Set<String> values) {
        PrefsBridge.putStringSet(key, values);
    }

    @Override
    public void putInt(@NonNull String key, int value) {
        PrefsBridge.putInt(key, value);
    }

    @Override
    public void putLong(@NonNull String key, long value) {
        PrefsBridge.putLong(key, value);
    }

    @Override
    public void putFloat(@NonNull String key, float value) {
        PrefsBridge.putFloat(key, value);
    }

    @Override
    public void putBoolean(@NonNull String key, boolean value) {
        PrefsBridge.putBoolean(key, value);
    }

    @Override
    @Nullable
    public String getString(@NonNull String key, @Nullable String defValue) {
        return PrefsBridge.getString(key, defValue);
    }

    @Override
    public boolean getBoolean(@NonNull String key, boolean defValue) {
        return PrefsBridge.getBoolean(key, defValue);
    }

    @Override
    public int getInt(@NonNull String key, int defValue) {
        return PrefsBridge.getInt(key, defValue);
    }

    @Override
    public long getLong(@NonNull String key, long defValue) {
        return PrefsBridge.getLong(key, defValue);
    }

    @Override
    public float getFloat(@NonNull String key, float defValue) {
        return PrefsBridge.getFloat(key, defValue);
    }

    @Override
    @Nullable
    public Set<String> getStringSet(@NonNull String key, @Nullable Set<String> defValues) {
        return PrefsBridge.getStringSet(key, defValues);
    }
}
