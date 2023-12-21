package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;

public class StatusBarSettings extends SettingsPreferenceFragment {

    Preference mDeviceStatus; // 硬件指示器
    Preference mToastStatus; // 灵动 Toast

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar;
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
        mDeviceStatus = findPreference("prefs_key_system_ui_status_bar_device");
        mToastStatus = findPreference("prefs_key_system_ui_status_bar_toast");

        mDeviceStatus.setVisible(!isAndroidVersion(30));
        mToastStatus.setVisible(isHyperOSVersion(1f));

        mDeviceStatus.setEnabled(!isHyperOSVersion(1f)); // 临时禁用
    }
}
