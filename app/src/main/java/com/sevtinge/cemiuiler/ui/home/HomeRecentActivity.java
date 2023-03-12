package com.sevtinge.cemiuiler.ui.home;

import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;

public class HomeRecentActivity extends BaseHomeActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.home.HomeRecentActivity.HomeRecentFragment();
    }

    public static class HomeRecentFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.home_recent;
        }
    }

}
