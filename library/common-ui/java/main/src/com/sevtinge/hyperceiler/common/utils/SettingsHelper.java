package com.sevtinge.hyperceiler.common.utils;

import androidx.preference.PreferenceManager;

public class SettingsHelper {

    public static void initSharedPreferences(PreferenceManager preferenceManager,
                                    String sharedPreferencesName, int sharedPreferencesMode) {
        preferenceManager.setSharedPreferencesName(sharedPreferencesName);
        preferenceManager.setSharedPreferencesMode(sharedPreferencesMode);
        preferenceManager.setStorageDeviceProtected();
    }
}
