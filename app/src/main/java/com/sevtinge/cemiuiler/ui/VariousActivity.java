package com.sevtinge.cemiuiler.ui;

import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class VariousActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Fragment initFragment() {
        return new VariousFragment();
    }

    public static class VariousFragment extends SubFragment {

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

            mDisableBluetoothRestrict.setVisible(SdkHelper.IS_MIUI_14 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
            mDisableDeviceLog.setVisible(SdkHelper.isAndroidTiramisu());

            mDisableBluetoothRestrict.setOnPreferenceChangeListener((preference, o) -> true);
            mDisableDeviceLog.setOnPreferenceChangeListener((preference, o) -> true);
        }
    }
}
