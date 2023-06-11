package com.sevtinge.cemiuiler.ui.fragment.systemui;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.devicesdk.SdkHelper;

import moralnorm.preference.PreferenceCategory;

public class SystemUIOtherSettings extends SettingsPreferenceFragment {

    PreferenceCategory mMonetOverlay;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_other;
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
        mMonetOverlay = findPreference("prefs_key_system_ui_monet");
        mMonetOverlay.setVisible(!SdkHelper.isAndroidR());
    }
}
