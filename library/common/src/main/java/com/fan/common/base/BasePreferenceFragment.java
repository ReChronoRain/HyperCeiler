package com.fan.common.base;

import android.os.Bundle;

import androidx.annotation.Nullable;

import fan.preference.PreferenceFragment;

public abstract class BasePreferenceFragment extends PreferenceFragment {

    public abstract int getPreferenceScreenResId();

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        if (getPreferenceScreenResId() != 0) {
            setPreferencesFromResource(getPreferenceScreenResId(), rootKey);
        }
        initPrefs();
    }

    public void initPrefs() {}
}
