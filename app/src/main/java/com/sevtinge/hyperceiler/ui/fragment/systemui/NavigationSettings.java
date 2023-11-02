package com.sevtinge.hyperceiler.ui.fragment.systemui;

import android.provider.Settings;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class NavigationSettings extends SettingsPreferenceFragment {
    SwitchPreference customNav;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_navigation;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    private boolean isGestureNavigationEnabled() {
        var defaultNavigationMode = 0;
        var gestureNavigationMode = 2;

        return Settings.Secure.getInt(requireContext().getContentResolver(), "navigation_mode", defaultNavigationMode) == gestureNavigationMode;
    }

    @Override
    public void initPrefs() {
        customNav = findPreference("prefs_key_system_ui_navigation_custom");
        if (customNav != null) {
            customNav.setEnabled(!isGestureNavigationEnabled());
            customNav.setVisible(customNav.isEnabled());
        }

    }
}
