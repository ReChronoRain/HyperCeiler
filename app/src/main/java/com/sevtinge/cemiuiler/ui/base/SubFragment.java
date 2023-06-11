package com.sevtinge.cemiuiler.ui.base;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.Preference;

public abstract class SubFragment extends BasePreferenceFragment {

    public int mContentResId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContentResId = getContentResId();
        if (mContentResId != 0) {
            super.onCreate(savedInstanceState, mContentResId);
            addPreferencesFromResource(mContentResId);
        } else {
            super.onCreate(savedInstanceState);
        }
        initPrefs();
    }

    public abstract int getContentResId();


    public void initPrefs() {
    }


    public SharedPreferences getSharedPreferences() {
        return PrefsUtils.mSharedPreferences;
    }
}
