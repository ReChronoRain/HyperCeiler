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
package com.sevtinge.hyperceiler.ui.fragment.framework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class FreeFormSettings extends SettingsPreferenceFragment {

    SwitchPreference mMoreFreeForm; // 多小窗
    PreferenceCategory mSmallFreeForm; // 小窗气泡

    @Override
    public int getContentResId() {
        return R.xml.framework_freeform;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }

    @Override
    public void initPrefs() {
        mMoreFreeForm = findPreference("prefs_key_system_framework_freeform_count");
        mSmallFreeForm = findPreference("prefs_key_system_framework_freeform_bubble_title");

        mMoreFreeForm.setVisible(isMoreMiuiVersion(13f) || isMoreHyperOSVersion(1f));
        mSmallFreeForm.setVisible(!isAndroidVersion(30));
    }
}
