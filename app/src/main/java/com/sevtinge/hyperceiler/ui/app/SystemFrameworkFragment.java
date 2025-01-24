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
package com.sevtinge.hyperceiler.ui.app;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.devicesdk.TelephonyManager;

import androidx.preference.Preference;

public class SystemFrameworkFragment extends DashboardFragment {

    Preference mNetwork;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.framework;
    }

    @Override
    public void initPrefs() {
        mNetwork = findPreference("prefs_key_system_framework_network");
        mNetwork.setVisible(TelephonyManager.getDefault().isFiveGCapable());
    }
}
