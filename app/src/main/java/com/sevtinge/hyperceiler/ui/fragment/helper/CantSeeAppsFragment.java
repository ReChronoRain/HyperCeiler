package com.sevtinge.hyperceiler.ui.fragment.helper;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.ui.MainActivityContextHelper;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;

public class CantSeeAppsFragment extends SettingsPreferenceFragment {

    Preference mHelpCantSeeApps;

    @Override
    public int getContentResId() {
        return R.xml.prefs_help_cant_see_apps;
    }

    @Override
    public void initPrefs() {
        setTitle(R.string.help);
        mHelpCantSeeApps = findPreference("prefs_key_textview_help_cant_see_apps");
        if (mHelpCantSeeApps != null) {
            if (PreferenceHeader.mUninstallApp != null && PreferenceHeader.mDisableOrHiddenApp != null) mHelpCantSeeApps.setSummary(getString(R.string.help_cant_see_apps_desc) + String.join("\n", PreferenceHeader.mUninstallApp) + "\n" + String.join("\n", PreferenceHeader.mDisableOrHiddenApp));
            else if (PreferenceHeader.mUninstallApp == null && PreferenceHeader.mDisableOrHiddenApp != null) mHelpCantSeeApps.setSummary(getString(R.string.help_cant_see_apps_desc) + String.join("\n", PreferenceHeader.mUninstallApp));
            else if (PreferenceHeader.mUninstallApp != null && PreferenceHeader.mDisableOrHiddenApp == null) mHelpCantSeeApps.setSummary(getString(R.string.help_cant_see_apps_desc) + String.join("\n", PreferenceHeader.mDisableOrHiddenApp));
            else mHelpCantSeeApps.setSummary(getString(R.string.help_cant_see_apps_desc_no_hidden));
        }
    }
}
