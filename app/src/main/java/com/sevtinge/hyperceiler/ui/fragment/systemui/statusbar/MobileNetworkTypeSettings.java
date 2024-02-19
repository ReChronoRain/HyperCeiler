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

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class MobileNetworkTypeSettings extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mMobileMode;
    PreferenceCategory mMobileTypeGroup;
    SwitchPreference mMobileType;
    SwitchPreference isOnlyShowNetworkCard;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_mobile_network_type;
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
        boolean doubleLine = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_statusbar_network_icon_enable", false);
        int mobileMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_status_bar_icon_mobile_network_type", "0"));
        isOnlyShowNetworkCard = findPreference("prefs_key_system_ui_statusbar_mobile_type_only_show_network");
        mMobileMode = findPreference("prefs_key_system_ui_status_bar_icon_mobile_network_type");
        mMobileType = findPreference("prefs_key_system_ui_statusbar_mobile_type_enable");
        mMobileTypeGroup = findPreference("prefs_key_system_ui_statusbar_mobile_type_group");

        if (doubleLine) {
            isOnlyShowNetworkCard.setEnabled(!doubleLine);
            isOnlyShowNetworkCard.setSummary(R.string.system_ui_status_bar_mobile_type_only_show_network_desc);
        }

        setMobileMode(mobileMode);
        mMobileMode.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mMobileMode) {
            setMobileMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setMobileMode(int mode) {
        mMobileType.setEnabled(mode != 2);
        if (mode == 2) {
            mMobileTypeGroup.setVisible(false);
            mMobileType.setChecked(false);
            mMobileType.setSummary(R.string.system_ui_status_bar_mobile_type_single_desc);
        } else {
            mMobileType.setSummary("");
        }
    }
}
