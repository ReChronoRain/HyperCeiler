package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidT;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.os.Build;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class VariousFragment extends SettingsPreferenceFragment {

    SwitchPreference mDisableBluetoothRestrict; // 禁用蓝牙临时关闭
    SwitchPreference mDisableDeviceLog; // 关闭访问设备日志确认

    @Override
    public int getContentResId() {
        return R.xml.various;
    }

    @Override
    public void initPrefs() {
        mDisableBluetoothRestrict = findPreference("prefs_key_various_disable_bluetooth_restrict");
        mDisableDeviceLog = findPreference("prefs_key_various_disable_access_device_logs");

        mDisableBluetoothRestrict.setVisible(isMoreMiuiVersion(14f) && isMoreAndroidVersion(Build.VERSION_CODES.S));
        mDisableDeviceLog.setVisible(isAndroidT());

        mDisableBluetoothRestrict.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableDeviceLog.setOnPreferenceChangeListener((preference, o) -> true);
    }
}
