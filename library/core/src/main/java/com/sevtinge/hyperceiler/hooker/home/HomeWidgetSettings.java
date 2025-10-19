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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hooker.home;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.core.R;

public class HomeWidgetSettings extends DashboardFragment {

    SwitchPreference mMoveToMinusOneScreen;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_widget;
    }

    @Override
    public void initPrefs() {
        mMoveToMinusOneScreen = findPreference("prefs_key_home_widget_allow_moved_to_minus_one_screen");
        if (isPad()) setFuncHint(mMoveToMinusOneScreen, 1);
    }
}
