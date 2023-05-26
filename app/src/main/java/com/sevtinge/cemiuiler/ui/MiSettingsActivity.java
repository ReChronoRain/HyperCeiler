package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class MiSettingsActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.MiSettingsActivity.MiSettingsFragment();
    }

    public static class MiSettingsFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.mi_settings;
        }
    }
}