package com.fan.common.base;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.utils.PrefsConfigurator;
import com.sevtinge.hyperceiler.common.utils.SettingsHelper;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import fan.preference.PreferenceFragment;

public abstract class BasePreferenceFragment extends PreferenceFragment {

    public abstract int getPreferenceScreenResId();

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        PrefsConfigurator.setup(this);
        if (getPreferenceScreenResId() != 0) {
            setPreferencesFromResource(getPreferenceScreenResId(), rootKey);
        }
        initPrefs();
    }

    public void initPrefs() {}
}
