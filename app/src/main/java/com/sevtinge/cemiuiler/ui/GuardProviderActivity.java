package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class GuardProviderActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.GuardProviderActivity.GuardProviderFragment();
    }

    public static class GuardProviderFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.guard_provider;
        }
    }


}
