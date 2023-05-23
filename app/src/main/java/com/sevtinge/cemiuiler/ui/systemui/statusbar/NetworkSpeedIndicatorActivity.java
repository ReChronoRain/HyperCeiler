package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SeekBarPreference;

public class NetworkSpeedIndicatorActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new NetworkSpeedIndicatorActivity.NetworkSpeedIndicatorFragment();
    }

    public static class NetworkSpeedIndicatorFragment extends SubFragment {

        SeekBarPreference mNetworkSpeedWidth; // 固定宽度

        @Override
        public int getContentResId() {
            return R.xml.system_ui_status_bar_network_speed_indicator;
        }

        @Override
        public void initPrefs() {
            mNetworkSpeedWidth = findPreference("prefs_key_system_ui_statusbar_network_speed_fixedcontent_width");
            mNetworkSpeedWidth.setVisible(!SdkHelper.isAndroidR());
        }
    }
}
