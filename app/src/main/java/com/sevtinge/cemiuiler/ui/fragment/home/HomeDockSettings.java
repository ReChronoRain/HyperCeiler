package com.sevtinge.cemiuiler.ui.fragment.home;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.os.Build;
import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class HomeDockSettings extends SettingsPreferenceFragment {

    SwitchPreference mDisableRecentIcon;
    SwitchPreference mDockBackground;

    @Override
    public int getContentResId() {
        return R.xml.home_dock;
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
        mDisableRecentIcon = findPreference("prefs_key_home_dock_disable_recents_icon");
        mDockBackground = findPreference("prefs_key_home_dock_bg_custom_enable");
        mDisableRecentIcon.setVisible(isPad());
        mDockBackground.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.S));
        mDockBackground.setEnabled(mDockBackground.isVisible());
    }
}
