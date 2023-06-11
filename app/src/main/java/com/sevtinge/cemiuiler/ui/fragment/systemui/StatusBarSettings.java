package com.sevtinge.cemiuiler.ui.fragment.systemui;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.Preference;

public class StatusBarSettings extends SettingsPreferenceFragment {

    Preference mDeviceStatus; // 硬件指示器

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar;
    }

    @Override
    public void initPrefs() {
        mDeviceStatus = findPreference("prefs_key_system_ui_status_bar_device");
        mDeviceStatus.setVisible(!SdkHelper.isAndroidR());
    }
}
