package com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SeekBarPreferenceEx;

public class NetworkSpeedIndicatorSettings extends SettingsPreferenceFragment {

    SeekBarPreferenceEx mNetworkSpeedWidth; // 固定宽度

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_network_speed_indicator;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mNetworkSpeedWidth = findPreference("prefs_key_system_ui_statusbar_network_speed_fixedcontent_width");
        mNetworkSpeedWidth.setVisible(!isAndroidR());
    }
}
