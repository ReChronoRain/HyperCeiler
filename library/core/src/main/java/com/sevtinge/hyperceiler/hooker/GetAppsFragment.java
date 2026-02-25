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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hooker;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import androidx.preference.EditTextPreference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;

public class GetAppsFragment extends DashboardFragment {

    EditTextPreference mModel;
    EditTextPreference mDevice;
    EditTextPreference mManufacturer;
    SwitchPreference mRiskCheck;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.getapps;
    }

    @Override
    public void initPrefs() {
        mDevice = findPreference("prefs_key_market_device_modify_device");
        mModel = findPreference("prefs_key_market_device_modify_model");
        mManufacturer = findPreference("prefs_key_market_device_modify_manufacturer");
        mRiskCheck = findPreference("prefs_key_market_bypass_risk_check");

        if (isPad()) {
            setFuncHint(mRiskCheck, 1);
        }

        int currentValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_market_device_modify_new", "0"));

        if (currentValue == 1) {
            mDevice.setVisible(true);
            mModel.setVisible(true);
            mManufacturer.setVisible(true);
        } else {
            mDevice.setVisible(false);
            mModel.setVisible(false);
            mManufacturer.setVisible(false);
        }
    }
}
