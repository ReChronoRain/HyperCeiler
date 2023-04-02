package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;

public class BatteryDetailIndicatorActivity extends BaseSystemUIActivity {
    @Override
    public Fragment initFragment() {
        return new BatteryDetailIndicatorActivity.BatteryDetailIndicatorFragment();
    }

    public static class BatteryDetailIndicatorFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.system_ui_statusbar_battery_detail_indicator;
        }

    }
}
