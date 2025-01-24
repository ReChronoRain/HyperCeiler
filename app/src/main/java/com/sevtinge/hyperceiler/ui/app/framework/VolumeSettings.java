/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.app.framework;

import android.provider.Settings;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;

public class VolumeSettings extends DashboardFragment {

    DropDownPreference mDefaultVolumeStream;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.framework_volume;
    }

    @Override
    public void initPrefs() {
        mDefaultVolumeStream = findPreference("prefs_key_system_framework_default_volume_stream");

        mDefaultVolumeStream.setOnPreferenceChangeListener((preference, o) -> {
            Settings.Secure.putInt(getContext().getContentResolver(), "system_framework_default_volume_stream", Integer.parseInt((String) o));
            return true;
        });
    }
}
