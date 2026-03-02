package com.sevtinge.hyperceiler.provision.utils;

import android.content.Context;

import androidx.preference.PreferenceDataStore;

import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.util.Set;

public class PrefsUtilsDataStore extends PreferenceDataStore {

    private final Context context;

    public PrefsUtilsDataStore(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        PrefsBridge.putBoolean(key, value);
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return PrefsBridge.getBoolean(key, defValue);
    }

    @Override
    public void putString(String key, String value) {
        PrefsBridge.putString(key, value);
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public String getString(String key, String defValue) {
        return PrefsBridge.getString(key, defValue);
    }

    @Override
    public void putInt(String key, int value) {
        PrefsBridge.putInt(key, value);
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public int getInt(String key, int defValue) {
        return PrefsBridge.getInt(key, defValue);
    }

    @Override
    public void putStringSet(String key, Set<String> values) {
        PrefsBridge.putStringSet(key, values);
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Set<String> values = PrefsBridge.getStringSet(key, defValues);
        return values != null ? values : defValues;
    }
}

