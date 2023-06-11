package com.sevtinge.cemiuiler.ui.base;

import com.sevtinge.cemiuiler.ui.SubSettings;
import com.sevtinge.cemiuiler.ui.fragment.framework.OtherSettings;
import com.sevtinge.cemiuiler.ui.fragment.home.HomeDockSettings;
import com.sevtinge.cemiuiler.ui.fragment.home.HomeFolderSettings;
import com.sevtinge.cemiuiler.ui.fragment.home.HomeGestureSettings;
import com.sevtinge.cemiuiler.ui.fragment.sub.MultiActionSettings;
import com.sevtinge.cemiuiler.ui.fragment.various.AlertDialogSettings;

import moralnorm.preference.Preference;
import moralnorm.preference.material.MaterialPreferenceFragment;
import moralnorm.preference.material.MaterialPreferenceFragmentCompat;

public class SettingsActivity extends BaseSettingsActivity implements MaterialPreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Override
    public boolean onPreferenceStartFragment(MaterialPreferenceFragment preferenceFragment, Preference preference) {
        boolean isBundleEnable = preferenceFragment instanceof OtherSettings ||
            preferenceFragment instanceof HomeDockSettings ||
            preferenceFragment instanceof HomeFolderSettings ||
            preferenceFragment instanceof AlertDialogSettings ||
            preferenceFragment instanceof HomeGestureSettings ||
            preferenceFragment instanceof MultiActionSettings;
        onStartSettingsForArguments(preference, isBundleEnable);
        return true;
    }

    public void onStartSettingsForArguments(Preference preference, boolean isBundleEnable) {
        mProxy.onStartSettingsForArguments(SubSettings.class, preference, isBundleEnable);
    }
}
