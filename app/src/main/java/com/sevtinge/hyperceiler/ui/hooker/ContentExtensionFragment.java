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

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;

public class ContentExtensionFragment extends DashboardFragment {
    SwitchPreference mUnlockTaplus;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.content_extension;
    }

    @Override
    public void initPrefs() {
        mUnlockTaplus= findPreference("prefs_key_content_extension_unlock_taplus");

        mUnlockTaplus.setVisible(!isAndroidVersion(30));
    }
}
