package com.sevtinge.cemiuiler.ui.base;

import android.content.Context;
import android.os.Bundle;

import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.PreferenceFragmentCompat;
import moralnorm.preference.PreferenceManager;
import moralnorm.preference.material.MaterialPreferenceFragmentCompat;


public class BasePreferenceFragment extends MaterialPreferenceFragmentCompat {

    public void onCreate(Bundle savedInstanceState, int prefs_default) {
        super.onCreate(savedInstanceState);
        try {
            getPreferenceManager().setSharedPreferencesName(PrefsUtils.mPrefsName);
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            getPreferenceManager().setStorageDeviceProtected();
            PreferenceManager.setDefaultValues(getActivity(), prefs_default, false);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

    }
}
