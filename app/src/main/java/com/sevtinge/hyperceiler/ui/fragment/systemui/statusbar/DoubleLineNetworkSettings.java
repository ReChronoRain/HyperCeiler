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
package com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar;

import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;

public class DoubleLineNetworkSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    DropDownPreference mIconTheme;
    DropDownPreference mIconStyle;
    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_doubleline_network;
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
        mIconTheme = findPreference("prefs_key_system_ui_statusbar_iconmanage_mobile_network_icon_theme");
        mIconStyle = findPreference("prefs_key_system_ui_status_mobile_network_icon_style");

        //setCanBeVisible(mBlurMode);
        mIconTheme.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mIconTheme) {
            setCanBeVisible(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setCanBeVisible(int mode) {
        mIconStyle.setVisible(mode == 2);
    }
}
