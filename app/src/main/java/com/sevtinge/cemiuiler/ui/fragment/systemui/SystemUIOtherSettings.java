package com.sevtinge.cemiuiler.ui.fragment.systemui;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.PreferenceCategory;

public class SystemUIOtherSettings extends SettingsPreferenceFragment {

    PreferenceCategory mMonetOverlay;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_other;
    }

    @Override
    public void initPrefs() {
        mMonetOverlay = findPreference("prefs_key_system_ui_monet");
        mMonetOverlay.setVisible(!SdkHelper.isAndroidR());
    }
}
