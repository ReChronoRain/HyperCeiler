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
package com.sevtinge.hyperceiler.ui.sub.systemui.statusbar;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import fan.preference.Preference;
import fan.preference.PreferenceCategory;
import fan.preference.SeekBarPreferenceCompat;

public class ClockIndicatorSettings extends DashboardFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mClockModePreference;
    PreferenceCategory mDefault;
    PreferenceCategory mGeek;
    SeekBarPreferenceCompat mWidth;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_clock_indicator;
    }

    @Override
    public void initPrefs() {
        int mClockMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_statusbar_clock_mode", "0"));
        mClockModePreference = findPreference("prefs_key_system_ui_statusbar_clock_mode");
        mDefault = findPreference("prefs_key_system_ui_statusbar_clock_default");
        mGeek = findPreference("prefs_key_system_ui_statusbar_clock_geek");
        mWidth = findPreference("prefs_key_system_ui_statusbar_clock_fixedcontent_width");

        setClockMode(mClockMode);
        mClockModePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mClockModePreference) {
            setClockMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setClockMode(int mode) {
        mWidth.setVisible(mode != 0);
        mDefault.setVisible(mode == 1);
        mGeek.setVisible(mode == 2);
    }
}
