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
package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;

public class StatusBarSettings extends SettingsPreferenceFragment {

    Preference mDeviceStatus; // 硬件指示器
    Preference mToastStatus; // 灵动 Toast
    Preference mIconManagerOld;
    Preference mIconManagerNew;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mDeviceStatus = findPreference("prefs_key_system_ui_status_bar_device");
        mToastStatus = findPreference("prefs_key_system_ui_status_bar_toast");
        mIconManagerOld = findPreference("prefs_key_icon_manager_old");
        mIconManagerNew = findPreference("prefs_key_icon_manager_new");

        mDeviceStatus.setVisible(!isAndroidVersion(30) && !isHyperOSVersion(1f));
        mToastStatus.setVisible(isHyperOSVersion(1f));

        mIconManagerOld.setVisible(!isMoreHyperOSVersion(1f));
        mIconManagerNew.setVisible(isMoreHyperOSVersion(1f));
    }
}
