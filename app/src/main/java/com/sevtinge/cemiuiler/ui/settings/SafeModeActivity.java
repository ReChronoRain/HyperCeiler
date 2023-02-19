package com.sevtinge.cemiuiler.ui.settings;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class SafeModeActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new SafeModeFragment();
    }


    public static class SafeModeFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.settings_safe_mode;
        }
    }
}
