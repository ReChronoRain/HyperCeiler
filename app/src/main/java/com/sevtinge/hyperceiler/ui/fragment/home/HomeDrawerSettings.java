package com.sevtinge.hyperceiler.ui.fragment.home;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class HomeDrawerSettings extends SettingsPreferenceFragment {

    SwitchPreference mAllAppsContainerViewBlur;

    @Override
    public int getContentResId() {
        return R.xml.home_drawer;
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
        mAllAppsContainerViewBlur = findPreference("prefs_key_home_drawer_blur");
        mAllAppsContainerViewBlur.setVisible(!isAndroidVersion(30));

        mAllAppsContainerViewBlur.setOnPreferenceChangeListener((preference, o) -> true);
    }
}
