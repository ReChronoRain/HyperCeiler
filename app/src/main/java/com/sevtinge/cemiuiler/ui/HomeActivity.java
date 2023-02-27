package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class HomeActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new HomeFragment();
    }

    public static class HomeFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.home;
        }
    }
}
