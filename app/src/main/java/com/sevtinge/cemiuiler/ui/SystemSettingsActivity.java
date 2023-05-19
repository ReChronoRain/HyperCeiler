package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.PreferenceCategory;

public class SystemSettingsActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.SystemSettingsActivity.SystemSettingsFragment();
    }

    public static class SystemSettingsFragment extends SubFragment {

        PreferenceCategory mHighMode; //极致模式
        PreferenceCategory mAreaScreenshot; //区域截屏

        @Override
        public int getContentResId() {
            return R.xml.system_settings;
        }

        @Override
        public void initPrefs() {
            mHighMode = findPreference("prefs_key_system_settings_develop_speed");
            mAreaScreenshot = findPreference("prefs_key_system_settings_accessibility_title");
            mHighMode.setVisible(!SdkHelper.isAndroidR());
            mAreaScreenshot.setVisible(SdkHelper.isAndroidR());
        }
    }
}


