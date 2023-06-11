package com.sevtinge.cemiuiler.ui.fragment.home;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.devicesdk.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class HomeTitleSettings extends SettingsPreferenceFragment {

    SwitchPreference mDisableMonoChrome;
    SwitchPreference mDisableMonetColor;

    @Override
    public int getContentResId() {
        return R.xml.home_title;
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
        mDisableMonoChrome = findPreference("prefs_key_home_other_icon_mono_chrome");
        mDisableMonoChrome.setVisible(SdkHelper.isAndroidTiramisu());
        mDisableMonoChrome.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableMonetColor = findPreference("prefs_key_home_other_icon_monet_color");
        mDisableMonetColor.setVisible(SdkHelper.isAndroidTiramisu());
        mDisableMonetColor.setOnPreferenceChangeListener((preference, o) -> true);
    }
}
