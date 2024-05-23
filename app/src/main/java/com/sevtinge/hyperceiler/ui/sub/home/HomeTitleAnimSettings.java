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
package com.sevtinge.hyperceiler.ui.sub.home;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import fan.preference.Preference;

public class HomeTitleAnimSettings extends SettingsPreferenceFragment {
    Preference mPage1;
    Preference mPage9;
    @Override
    public int getContentResId() {
        return R.xml.home_title_anim;
    }


    @Override
    public void initPrefs() {
        mPage1 = findPreference("prefs_key_home_title_custom_anim_param_1_title");
        mPage9 = findPreference("prefs_key_home_title_custom_anim_param_9_title");

        mPage9.setVisible(isMoreHyperOSVersion(1f));
        mPage1.setVisible(!isMoreHyperOSVersion(1f));
    }
}
