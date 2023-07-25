package com.sevtinge.cemiuiler.ui.fragment.base;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceManager;
import moralnorm.preference.compat.PreferenceFragment;

public class BasePreferenceFragment extends PreferenceFragment {

    private PreferenceManager mPreferenceManager;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mPreferenceManager = getPreferenceManager();
        mPreferenceManager.setSharedPreferencesName(PrefsUtils.mPrefsName);
        mPreferenceManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
        mPreferenceManager.setStorageDeviceProtected();
    }

    public void setTitle(int titleResId) {
        setTitle(getResources().getString(titleResId));
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    public String getFragmentName(Fragment fragment) {
        return fragment.getClass().getName();
    }

    public String getPreferenceTitle(Preference preference) {
        return preference.getTitle().toString();
    }

    public String getPreferenceKey(Preference preference) {
        return preference.getKey();
    }

    public void finish() {
        getActivity().finish();
    }
}
