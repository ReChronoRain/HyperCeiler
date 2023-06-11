package com.sevtinge.cemiuiler.ui.fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class ThemeManagerFragment extends SettingsPreferenceFragment {

    PreferenceCategory mVersionCodeModifyPreferenceCat;
    SwitchPreference mVersionCodeModifyPreference;
    SwitchPreference mThemeManagerCrack;

    @Override
    public int getContentResId() {
        return R.xml.theme_manager;
    }

    @Override
    public void initPrefs() {
        mVersionCodeModifyPreferenceCat = findPreference("prefs_key_theme_manager_version_code_modify_cat");
        mVersionCodeModifyPreference = findPreference("prefs_key_theme_manager_version_code_modify");

        if (SdkHelper.PROP_MIUI_VERSION_CODE <= 13) {
            mVersionCodeModifyPreferenceCat.setVisible(true);
        } else {
            mVersionCodeModifyPreferenceCat.setVisible(false);
            mVersionCodeModifyPreference.setChecked(false);
            mVersionCodeModifyPreference.setEnabled(false);
        }

        mThemeManagerCrack = findPreference("prefs_key_various_theme_crack");
        if (!getSharedPreferences().getBoolean("prefs_key_hidden_function", false)) {
            mThemeManagerCrack.setVisible(false);
        }
    }
}
