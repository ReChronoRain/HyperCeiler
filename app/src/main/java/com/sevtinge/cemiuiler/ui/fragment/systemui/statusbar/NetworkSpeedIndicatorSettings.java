package com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SeekBarPreference;

public class NetworkSpeedIndicatorSettings extends SettingsPreferenceFragment {

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
