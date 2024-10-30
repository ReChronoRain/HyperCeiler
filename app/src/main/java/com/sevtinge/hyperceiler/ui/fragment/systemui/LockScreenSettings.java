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
package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.api.OldFunApisKt.isDeviceEncrypted;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.os.Build;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import androidx.preference.SwitchPreference;

public class LockScreenSettings extends SettingsPreferenceFragment {
    SwitchPreference mShowSec; // 时钟显示秒数
    SwitchPreference mForceSystemFonts; // 时钟使用系统字体
    SwitchPreference mPasswordFree; // 开机免输入密码
    SwitchPreference mChangingCVTime; // 充电信息显示刷新间隔

    @Override
    public int getContentResId() {
        return R.xml.system_ui_lock_screen;
    }

    @Override
    public void initPrefs() {
        mForceSystemFonts = findPreference("prefs_key_system_ui_lock_screen_force_system_fonts");
        mPasswordFree = findPreference("prefs_key_system_ui_lock_screen_password_free");
        mChangingCVTime = findPreference("prefs_key_system_ui_lock_screen_show_spacing_value");
        mShowSec = findPreference("prefs_key_system_ui_lock_screen_show_second");

        mShowSec.setVisible(!isHyperOSVersion(1f));
        mForceSystemFonts.setVisible(!isHyperOSVersion(1f));
        mChangingCVTime.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU));

        if (isDeviceEncrypted(requireContext())) {
            mPasswordFree.setChecked(false);
            mPasswordFree.setEnabled(false);
            mPasswordFree.setSummary(R.string.system_ui_lock_screen_password_free_tip);
        }
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }
}
