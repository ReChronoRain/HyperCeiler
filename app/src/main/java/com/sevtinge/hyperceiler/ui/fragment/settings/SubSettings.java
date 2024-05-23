package com.sevtinge.hyperceiler.ui.fragment.settings;

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
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);
        registerCoordinateScrollView(findViewById(R.id.main_content));
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