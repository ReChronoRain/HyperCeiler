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

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreSmallVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.R;

import fan.preference.DropDownPreference;

public class LockScreenSettings extends DashboardFragment {
    SwitchPreference mHideLeftButton; // 隐藏左侧按钮
    SwitchPreference mHideRightButton; // 隐藏右侧按钮
    SwitchPreference mBlockEditor; // 禁用长按进入锁屏编辑
    DropDownPreference mHideLeftButtonNew; // 左侧按钮自定义

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_lock_screen;
    }

    @Override
    public void initPrefs() {
        final boolean moreAndroidVersion = isMoreAndroidVersion(35);
        mBlockEditor = findPreference("prefs_key_system_ui_lock_screen_block_editor");

        mHideLeftButton = findPreference("prefs_key_system_ui_lock_screen_hide_smart_screen");
        mHideRightButton = findPreference("prefs_key_system_ui_lock_screen_hide_camera");
        mHideLeftButtonNew = findPreference("prefs_key_system_ui_lock_screen_bottom_left_button");

        mBlockEditor.setVisible(!moreAndroidVersion);
        mHideLeftButton.setVisible(!moreAndroidVersion);
        mHideLeftButtonNew.setVisible(moreAndroidVersion);

        if (isMoreSmallVersion(200, 2f)) {
            setFuncHint(mHideLeftButtonNew, 2);
            setFuncHint(mHideRightButton, 2);
        }
    }
}
