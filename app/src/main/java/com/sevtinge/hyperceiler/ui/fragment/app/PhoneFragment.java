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
package com.sevtinge.hyperceiler.ui.fragment.app;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import androidx.preference.Preference;

public class PhoneFragment extends SettingsPreferenceFragment {
    Preference mPhone;

    @Override
    public int getContentResId() {
        return R.xml.phone;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.phone),
            "com.android.phone"
        );
    }

    @Override
    public void initPrefs() {
        mPhone = findPreference("prefs_key_phone_additional_network_settings");
        mPhone.setOnPreferenceClickListener(
            preference -> {
                ShellInit.getShell().run("am start -n com.android.phone/.SwitchDebugActivity").sync();
                return true;
            }
        );
    }
}
