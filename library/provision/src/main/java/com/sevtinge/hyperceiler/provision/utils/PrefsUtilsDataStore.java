package com.sevtinge.hyperceiler.provision.utils;

import android.content.Context;

import androidx.preference.PreferenceDataStore;

import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.util.Set;

public class PrefsUtilsDataStore extends PreferenceDataStore {

    private final Context context;

    public PrefsUtilsDataStore(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        PrefsUtils.editor().putBoolean(key, value).apply();
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return PrefsUtils.getSharedBoolPrefs(context, key, defValue);
    }

    @Override
    public void putString(String key, String value) {
        PrefsUtils.editor().putString(key, value).apply();
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public String getString(String key, String defValue) {
        return PrefsUtils.getSharedStringPrefs(context, key, defValue);
    }

    @Override
    public void putInt(String key, int value) {
        PrefsUtils.editor().putInt(key, value).apply();
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public int getInt(String key, int defValue) {
        return PrefsUtils.getSharedIntPrefs(context, key, defValue);
    }

    @Override
    public void putStringSet(String key, Set<String> values) {
        PrefsUtils.editor().putStringSet(key, values).apply();
        PrefsUtils.handlePreferenceChanged(key);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return PrefsUtils.getSharedStringSetPrefs(context, key);
    }
}

