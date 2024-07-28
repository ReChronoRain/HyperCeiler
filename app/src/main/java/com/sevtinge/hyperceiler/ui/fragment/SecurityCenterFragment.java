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

import static com.hchen.hooktool.utils.SystemSDK.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;

public class SecurityCenterFragment extends SettingsPreferenceFragment {

    Preference mPrivacy;

    @Override
    public int getContentResId() {
        return R.xml.security_center;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
                isPad() ? getResources().getString(R.string.security_center_pad) : isMoreHyperOSVersion(1f) ? getResources().getString(R.string.security_center_hyperos) : getResources().getString(R.string.security_center),
                "com.miui.securitycenter"
        );
    }

    @Override
    public void initPrefs() {
        mPrivacy = findPreference("prefs_key_security_center_privacy_safety");
        // mPrivacy.setVisible(!isMoreHyperOSVersion(1f));
    }
}
