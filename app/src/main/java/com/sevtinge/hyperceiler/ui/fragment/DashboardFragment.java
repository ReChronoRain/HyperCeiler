package com.sevtinge.hyperceiler.ui.fragment;

import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class DashboardFragment extends SettingsPreferenceFragment {

    @Override
    public int getPreferenceScreenResId() {
        return mPreferenceResId != 0 ? mPreferenceResId : 0;
    }
}
