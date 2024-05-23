/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.navigator.page.settings;

import static com.sevtinge.hyperceiler.utils.PropUtils.getProp;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import fan.preference.Preference;
import fan.preference.SwitchPreference;

public class SafeModeFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    String mPkgList = getProp("persist.hyperceiler.crash.report");

    SwitchPreference mHome;
    SwitchPreference mSettings;
    SwitchPreference mSystemUi;
    SwitchPreference mSecurityCenter;
    SwitchPreference mDemo;

    @Override
    public int getContentResId() {
        return R.xml.prefs_settings_safe_mode;
    }

    @Override
    public void initPrefs() {
        mHome = findPreference("prefs_key_home_safe_mode_enable");
        mSettings = findPreference("prefs_key_system_settings_state");
        mSystemUi = findPreference("prefs_key_system_ui_safe_mode_enable");
        mSecurityCenter = findPreference("prefs_key_security_center_safe_mode_enable");
        mDemo = findPreference("prefs_key_demo_safe_mode_enable");
        mSecurityCenter.setTitle(isMoreHyperOSVersion(1f) ? R.string.security_center_hyperos : R.string.security_center);
        mDemo.setVisible(BuildConfig.BUILD_TYPE == "debug");
        mSystemUi.setChecked(mPkgList.contains("systemui"));
        mSettings.setChecked(mPkgList.contains("settings"));
        mHome.setChecked(mPkgList.contains("home"));
        mSecurityCenter.setChecked(mPkgList.contains("center"));
        mDemo.setChecked(mPkgList.contains("demo"));
        mHome.setOnPreferenceChangeListener(this);
        mDemo.setOnPreferenceChangeListener(this);
        mSettings.setOnPreferenceChangeListener(this);
        mSystemUi.setOnPreferenceChangeListener(this);
        mSecurityCenter.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        String key = "";
        if (preference == mHome) {
            key = "home";
        } else if (preference == mSettings) {
            key = "settings";
        } else if (preference == mSystemUi) {
            key = "systemui";
        } else if (preference == mSecurityCenter) {
            key = "center";
        } else if (preference == mDemo) {
            key = "demo";
        }
        if (!key.isEmpty()) {
            String mPkgList = getProp("persist.hyperceiler.crash.report");
            if ((boolean) o) {
                if (mPkgList.isEmpty()) {
                    mPkgList = key;
                } else {
                    mPkgList += "," + key;
                }
            } else {
                mPkgList = mPkgList.replace("," + key, "").replace(key, "");
            }
            ShellInit.getShell().run("setprop persist.hyperceiler.crash.report \"" + mPkgList + "\"").sync();
        }
        return true;
    }

}
