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
package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import fan.preference.SwitchPreference;

public class MiShareFragment extends SettingsPreferenceFragment {

    SwitchPreference mMiShareNotAuto;

    @Override
    public int getContentResId() {
        return R.xml.mishare;
    }

    @Override
    public void initPrefs() {
        mMiShareNotAuto = findPreference("prefs_key_disable_mishare_auto_off");
        /*int appVersionCode = getPackageVersionCode(lpparam);

        if (appVersionCode <= 21400) {
            mMiShareNotAuto.setSummary(R.string.app_version_not_supported);
            mMiShareNotAuto.setEnabled(false);
        }*/
    }
}
