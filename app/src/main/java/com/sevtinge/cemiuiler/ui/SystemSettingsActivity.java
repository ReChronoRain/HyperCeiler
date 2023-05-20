package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class SystemSettingsActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.SystemSettingsActivity.SystemSettingsFragment();
    }

    public static class SystemSettingsFragment extends SubFragment {

        PreferenceCategory mHighMode; //极致模式
        SwitchPreference mAreaScreenshot; //区域截屏
        SwitchPreference mKnuckleFunction; //指关节相关

        @Override
        public int getContentResId() {
            return R.xml.system_settings;
        }

        @Override
        public void initPrefs() {
            mHighMode = findPreference("prefs_key_system_settings_develop_speed");
            mAreaScreenshot = findPreference("prefs_key_system_settings_area_screenshot");
            mKnuckleFunction = findPreference("prefs_key_system_settings_knuckle_function");
            mHighMode.setVisible(!SdkHelper.isAndroidR());
            mAreaScreenshot.setVisible(SdkHelper.isAndroidR());
            mKnuckleFunction.setVisible(SdkHelper.PROP_MIUI_VERSION_CODE >= 13);
        }
    }
}


