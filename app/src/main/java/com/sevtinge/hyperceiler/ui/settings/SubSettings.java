package com.sevtinge.hyperceiler.ui.settings;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;

/**
 * Stub class for showing sub-settings; we can't use the main Settings class
 * since for our app it is a special singleTask class.
 */
public class SubSettings extends SettingsActivity {

    @Override
    protected int getOwnerTheme() {
        return R.style.Theme_Settings_SubSettings;
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Log.d("SubSettings", "Launching fragment " + fragmentName);
        return true;
    }
}