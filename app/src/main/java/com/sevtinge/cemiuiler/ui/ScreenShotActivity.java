package com.sevtinge.cemiuiler.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class ScreenShotActivity extends BaseAppCompatActivity {

        @Override
        public Fragment initFragment() {
            return new com.sevtinge.cemiuiler.ui.ScreenShotActivity.ScreenShotFragment();
        }

        public static class ScreenShotFragment extends SubFragment {

            @Override
            public int getContentResId() {
                return R.xml.screenshot;
            }

            @Override
            public void initPrefs() {
                String format = BaseHook.mPrefsMap.getString("prefs_key_screenshot_format", "2");
                findPreference("prefs_key_screenshot_quality").setEnabled("2".equals(format) || "4".equals(format));
                findPreference("prefs_key_screenshot_format").setOnPreferenceChangeListener((preference, newValue) -> {
                    findPreference("prefs_key_screenshot_quality").setEnabled("2".equals(newValue) || "4".equals(newValue));
                    return true;
                });
            }
        }


    }


