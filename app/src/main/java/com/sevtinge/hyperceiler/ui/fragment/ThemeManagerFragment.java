package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.BuildUtils.getBuildType;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.view.View;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

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
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.theme_manager),
            "com.android.thememanager"
        );
    }

    @Override
    public void initPrefs() {
        mVersionCodeModifyPreferenceCat = findPreference("prefs_key_theme_manager_version_code_modify_cat");
        mVersionCodeModifyPreference = findPreference("prefs_key_theme_manager_version_code_modify");

        if (!isMoreMiuiVersion(13f)) {
            mVersionCodeModifyPreferenceCat.setVisible(true);
        } else {
            mVersionCodeModifyPreferenceCat.setVisible(false);
            mVersionCodeModifyPreference.setChecked(false);
            mVersionCodeModifyPreference.setEnabled(false);
        }

        mThemeManagerCrack = findPreference("prefs_key_various_theme_crack");
        if (!getSharedPreferences().getBoolean("prefs_key_various_enable_super_function", false)) {
            if (getBuildType().equals("debug")) {
                mThemeManagerCrack.setVisible(false);
            }
        }
    }
}
