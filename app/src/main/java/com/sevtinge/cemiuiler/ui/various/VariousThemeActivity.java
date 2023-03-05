package com.sevtinge.cemiuiler.ui.various;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;


import moralnorm.os.SystemProperties;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class VariousThemeActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new VariousThemeFragment();
    }

    public static class VariousThemeFragment extends SubFragment {

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

            if (getMiuiVersionCode() <= 13) {
                mVersionCodeModifyPreferenceCat.setVisible(true);
            } else {
                mVersionCodeModifyPreferenceCat.setVisible(false);
                mVersionCodeModifyPreference.setChecked(false);
                mVersionCodeModifyPreference.setEnabled(false);
            }

            mThemeManagerCrack = findPreference("prefs_key_various_theme_crack");
            if (!getSharedPreferences().getBoolean("prefs_key_hidden_function",false)) {
                mThemeManagerCrack.setVisible(false);
            }
        }

        private int getMiuiVersionCode() {
            return Integer.parseInt(SystemProperties.get("ro.miui.ui.version.code"));
        }
    }
}
