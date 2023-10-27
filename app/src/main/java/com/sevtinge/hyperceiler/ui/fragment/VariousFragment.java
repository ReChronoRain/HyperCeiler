package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidT;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class VariousFragment extends SettingsPreferenceFragment {

    SwitchPreference mDisableBluetoothRestrict; // 禁用蓝牙临时关闭
    SwitchPreference mDisableDeviceLog; // 关闭访问设备日志确认
    Preference mMipad; // 平板相关功能

    @Override
    public int getContentResId() {
        return R.xml.various;
    }

    @Override
    public void initPrefs() {
        mDisableBluetoothRestrict = findPreference("prefs_key_various_disable_bluetooth_restrict");
        mDisableDeviceLog = findPreference("prefs_key_various_disable_access_device_logs");
        mMipad = findPreference("prefs_key_various_mipad");

        mDisableBluetoothRestrict.setVisible(isMoreMiuiVersion(14f) && isMoreAndroidVersion(31));
        mDisableDeviceLog.setVisible(isAndroidT());
        mMipad.setVisible(isPad());

        mDisableBluetoothRestrict.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableDeviceLog.setOnPreferenceChangeListener((preference, o) -> true);
    }
}
