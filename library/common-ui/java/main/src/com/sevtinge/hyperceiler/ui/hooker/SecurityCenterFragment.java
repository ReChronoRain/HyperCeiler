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
package com.sevtinge.hyperceiler.ui.hooker;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class SecurityCenterFragment extends DashboardFragment {

    Preference mPrivacy;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.security_center;
    }

    @Override
    public void initPrefs() {
        mPrivacy = findPreference("prefs_key_security_center_privacy_safety");
        // mPrivacy.setVisible(!isMoreHyperOSVersion(1f));
    }
}
