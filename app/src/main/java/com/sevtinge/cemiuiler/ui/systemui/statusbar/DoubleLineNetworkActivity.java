package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;

public class DoubleLineNetworkActivity extends BaseSystemUIActivity {
    @Override
    public Fragment initFragment() {
        return new DoubleLineNetworkActivity.DoubleLineNetworkFragment();
    }

    public static class DoubleLineNetworkFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.system_ui_status_bar_doubleline_network;
        }

    }
}
