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
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.EditTextPreference;

public class GetAppsFragment extends SettingsPreferenceFragment {

    DropDownPreference mDeviceModify;
    EditTextPreference mModel;
    EditTextPreference mDevice;
    EditTextPreference mManufacture;

    @Override
    public int getContentResId() {
        return R.xml.getapps;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.market),
            "com.xiaomi.market"
        );
    }

    @Override
    public void initPrefs() {
        mDeviceModify = findPreference("prefs_key_market_device_modify_new");
        mDevice = findPreference("prefs_key_market_device_modify_device");
        mModel = findPreference("prefs_key_market_device_modify_model");
        mManufacture = findPreference("prefs_key_market_device_modify_manufacture");

        if (Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_market_device_modify_new", "0")) == 1) {
            mDevice.setVisible(true);
            mModel.setVisible(true);
            mManufacture.setVisible(true);
        } else {
            mDevice.setVisible(false);
            mModel.setVisible(false);
            mManufacture.setVisible(false);
        }

        mDeviceModify.setOnPreferenceChangeListener((preference, o) -> {
            if (Integer.parseInt((String) o) == 1) {
                mDevice.setVisible(true);
                mModel.setVisible(true);
                mManufacture.setVisible(true);
            } else {
                mDevice.setVisible(false);
                mModel.setVisible(false);
                mManufacture.setVisible(false);
            }
            return true;
        });
    }
}
