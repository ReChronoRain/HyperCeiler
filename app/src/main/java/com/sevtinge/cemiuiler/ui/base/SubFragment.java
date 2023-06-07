package com.sevtinge.cemiuiler.ui.base;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.sevtinge.cemiuiler.ui.PickerHomeActivity;
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

    public void openMultiAction(Preference preference, Bundle args, PickerHomeActivity.Actions actions) {
        Intent intent = new Intent(getContext(), PickerHomeActivity.class);
        if (args == null) args = new Bundle();
        args.putString("title", preference.getTitle().toString());
        args.putString("key", preference.getKey());
        args.putInt("actions", actions.ordinal());
        intent.putExtras(args);
        startActivity(intent);
    }

    public void openSubFragment(Bundle args, PickerHomeActivity.Actions actions) {
        Intent intent = new Intent(getContext(), PickerHomeActivity.class);
        if (args == null) args = new Bundle();
        args.putInt("actions", actions.ordinal());
        intent.putExtras(args);
        startActivity(intent);
    }


    public SharedPreferences getSharedPreferences() {
        return PrefsUtils.mSharedPreferences;
    }
}
