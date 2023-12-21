package com.sevtinge.hyperceiler.ui.fragment.home;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class HomeLayoutSettings extends SettingsPreferenceFragment {
    SwitchPreference mEnableFold;

    @Override
    public int getContentResId() {
        return R.xml.home_layout;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.mihome),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mEnableFold = findPreference("prefs_key_personal_assistant_overlap_mode");

        mEnableFold.setVisible(!isAndroidVersion(30));
    }
}
