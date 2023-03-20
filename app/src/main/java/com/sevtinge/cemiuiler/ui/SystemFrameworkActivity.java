package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;

import com.sevtinge.cemiuiler.ui.base.SubFragment;

import moralnorm.os.SdkVersion;
import moralnorm.preference.SwitchPreference;


public class SystemFrameworkActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new SystemFrameworkActivity.SystemFrameworkFragment();
    }

    public static class SystemFrameworkFragment extends SubFragment {
        @Override
        public int getContentResId() {
            return R.xml.system_framework;
        }

        @Override
        public void initPrefs() {
            SwitchPreference mOriginalAnimation;

            mOriginalAnimation = findPreference("prefs_key_system_framework_other_use_original_animation");
            mOriginalAnimation.setVisible(SdkVersion.isAndroidS || SdkVersion.isAndroidR);
        }
    }
}
