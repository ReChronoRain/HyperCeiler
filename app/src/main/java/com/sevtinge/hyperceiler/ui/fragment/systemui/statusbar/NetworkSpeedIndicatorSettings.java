package com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class NetworkSpeedIndicatorSettings extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    SeekBarPreferenceEx mNetworkSpeedWidth; // 固定宽度
    SwitchPreference mNetworkSwapIcon;
    SwitchPreference mNetworkSpeedSeparator;
    DropDownPreference mNetworkStyle;
    DropDownPreference mNetworkIcon;

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
        int mNetworkMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_statusbar_network_speed_style", "0"));
        mNetworkSpeedWidth = findPreference("prefs_key_system_ui_statusbar_network_speed_fixedcontent_width");
        mNetworkStyle = findPreference("prefs_key_system_ui_statusbar_network_speed_style");
        mNetworkIcon = findPreference("prefs_key_system_ui_statusbar_network_speed_icon");
        mNetworkSwapIcon = findPreference("prefs_key_system_ui_statusbar_network_speed_swap_places");
        mNetworkSpeedSeparator = findPreference("prefs_key_system_ui_status_bar_no_netspeed_separator");
        mNetworkSpeedWidth.setVisible(!isAndroidVersion(30));
        mNetworkSpeedSeparator.setVisible(!isHyperOSVersion(1f));

        setNetworkMode(mNetworkMode);
        mNetworkStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mNetworkStyle) {
            setNetworkMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setNetworkMode(int mode) {
        mNetworkIcon.setVisible(mode == 3 || mode == 4);
        mNetworkSwapIcon.setVisible(mode == 3 || mode == 4);
    }
}
