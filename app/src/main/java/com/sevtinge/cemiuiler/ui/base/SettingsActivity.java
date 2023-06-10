package com.sevtinge.cemiuiler.ui.base;

import com.sevtinge.cemiuiler.ui.SubSettings;

import moralnorm.preference.Preference;
import moralnorm.preference.material.MaterialPreferenceFragment;
import moralnorm.preference.material.MaterialPreferenceFragmentCompat;

public class SettingsActivity extends BaseSettingsActivity implements MaterialPreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Override
    public boolean onPreferenceStartFragment(MaterialPreferenceFragment preferenceFragment, Preference preference) {
        /*boolean isBundleEnable = preferenceFragment instanceof GestureFragment ||
            preferenceFragment instanceof DockFragment ||
            preferenceFragment instanceof OtherSettingsFragment ||
            preferenceFragment instanceof com.moralnorm.miuiext.ui.fragment.securitycenter.OtherSettingsFragment;*/
        onStartSettingsForArguments(preference, false);
        return true;
    }

    public void onStartSettingsForArguments(Preference preference, boolean isBundleEnable) {
        mProxy.onStartSettingsForArguments(SubSettings.class, preference, isBundleEnable);
    }
}
