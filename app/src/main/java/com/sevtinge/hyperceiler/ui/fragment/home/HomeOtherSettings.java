package com.sevtinge.hyperceiler.ui.fragment.home;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidR;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidT;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class HomeOtherSettings extends SettingsPreferenceFragment {

    SwitchPreference mFixAndroidRS;
    SwitchPreference mEnableMoreSettings;
    SwitchPreference mEnableFold;

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
        mEnableMoreSettings = findPreference("prefs_key_home_other_mi_pad_enable_more_setting");
        mEnableFold = findPreference("prefs_key_personal_assistant_overlap_mode");

        mFixAndroidRS.setVisible(!isAndroidT());
        mEnableMoreSettings.setVisible(isPad());
        mEnableFold.setVisible(!isAndroidR());
    }
}
