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
package com.sevtinge.hyperceiler.ui.hooker.framework;

import android.widget.SeekBar;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;

import fan.preference.SeekBarPreferenceCompat;

public class DisplaySettings extends DashboardFragment {

    SeekBarPreferenceCompat minBrightness;
    SeekBarPreferenceCompat maxBrightness;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.framework_display;
    }

    @Override
    public void initPrefs() {
        maxBrightness = findPreference("pref_key_system_ui_auto_brightness_max");
        minBrightness = findPreference("pref_key_system_ui_auto_brightness_min");
        assert minBrightness != null;
        minBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (maxBrightness.getValue() <= progress) maxBrightness.setValue(progress + 1);
                maxBrightness.setMinValue(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
}
