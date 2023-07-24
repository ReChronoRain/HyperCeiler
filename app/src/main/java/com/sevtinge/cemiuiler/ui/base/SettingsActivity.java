package com.sevtinge.cemiuiler.ui.base;

import androidx.annotation.NonNull;

import com.sevtinge.cemiuiler.ui.SubSettings;
import com.sevtinge.cemiuiler.ui.fragment.framework.OtherSettings;
import com.sevtinge.cemiuiler.ui.fragment.home.HomeDockSettings;
import com.sevtinge.cemiuiler.ui.fragment.home.HomeFolderSettings;
import com.sevtinge.cemiuiler.ui.fragment.home.HomeGestureSettings;
import com.sevtinge.cemiuiler.ui.fragment.sub.MultiActionSettings;
import com.sevtinge.cemiuiler.ui.fragment.various.AlertDialogSettings;

import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceFragmentCompat;
import moralnorm.preference.compat.PreferenceFragment;

public class SettingsActivity extends BaseSettingsActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public void onStartSettingsForArguments(Preference preference, boolean isBundleEnable) {
        mProxy.onStartSettingsForArguments(SubSettings.class, preference, isBundleEnable);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat preferenceFragmentCompat, @NonNull Preference preference) {
        boolean isBundleEnable = preferenceFragmentCompat instanceof OtherSettings ||
            preferenceFragmentCompat instanceof HomeDockSettings ||
            preferenceFragmentCompat instanceof HomeFolderSettings ||
            preferenceFragmentCompat instanceof AlertDialogSettings ||
            preferenceFragmentCompat instanceof HomeGestureSettings ||
            preferenceFragmentCompat instanceof MultiActionSettings;
        onStartSettingsForArguments(preference, isBundleEnable);
        return true;
    }
}
