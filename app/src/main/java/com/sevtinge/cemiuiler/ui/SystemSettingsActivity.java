package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.Preference;

public class SystemSettingsActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.SystemSettingsActivity.SystemSettingsFragment();
    }

    public static class SystemSettingsFragment extends SubFragment {

        Preference mHighMode; //极致模式

        @Override
        public int getContentResId() {
            return R.xml.system_settings;
        }

        @Override
        public void initPrefs() {
            mHighMode = findPreference("prefs_key_system_settings_develop_speed");
            mHighMode.setVisible(!SdkHelper.isAndroidR());
        }
    }
}


