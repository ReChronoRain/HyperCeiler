package com.sevtinge.cemiuiler.ui.fragment.home;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.devicesdk.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class HomeOtherSettings extends SettingsPreferenceFragment {

    SwitchPreference mFixAndroidRS;

    @Override
    public int getContentResId() {
        return R.xml.home_other;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.home),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mFixAndroidRS = findPreference("prefs_key_home_other_fix_android_r_s");
        mFixAndroidRS.setVisible(!SdkHelper.isAndroidTiramisu());
    }
}
