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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.hooker.systemui;

import static com.sevtinge.hyperceiler.hook.utils.api.OldFunApisKt.isDeviceEncrypted;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;

public class LockScreenSettings extends DashboardFragment {
    SwitchPreference mHideLeftButton; // 隐藏左侧按钮
    SwitchPreference mPasswordFree; // 开机免输入密码
    SwitchPreference mBlockEditor; // 禁用长按进入锁屏编辑
    DropDownPreference mHideLeftButtonNew; // 左侧按钮自定义

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_lock_screen;
    }

    @Override
    public void initPrefs() {
        final boolean moreAndroidVersion = isMoreAndroidVersion(35);
        mPasswordFree = findPreference("prefs_key_system_ui_lock_screen_password_free");
        mBlockEditor = findPreference("prefs_key_system_ui_lock_screen_block_editor");

        mHideLeftButton = findPreference("prefs_key_system_ui_lock_screen_hide_smart_screen");
        mHideLeftButtonNew = findPreference("prefs_key_system_ui_lock_screen_bottom_left_button");

        mBlockEditor.setVisible(!moreAndroidVersion);
        mHideLeftButton.setVisible(!moreAndroidVersion);
        mHideLeftButtonNew.setVisible(moreAndroidVersion);

        if (isDeviceEncrypted(requireContext())) {
            mPasswordFree.setChecked(false);
            mPasswordFree.setEnabled(false);
            mPasswordFree.setSummary(R.string.system_ui_lock_screen_password_free_tip);
        }
    }
}
