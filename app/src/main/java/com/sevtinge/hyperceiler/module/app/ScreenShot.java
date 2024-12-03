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
package com.sevtinge.hyperceiler.module.app;

import android.text.TextUtils;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.screenshot.DeviceShellCustomize;
import com.sevtinge.hyperceiler.module.hook.screenshot.SaveToPictures;
import com.sevtinge.hyperceiler.module.hook.screenshot.UnlockMinimumCropLimit;
import com.sevtinge.hyperceiler.module.hook.screenshot.UnlockPrivacyMarking;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

@HookBase(targetPackage = "com.miui.screenshot",  isPad = false)
public class ScreenShot extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockMinimumCropLimit(), mPrefsMap.getBoolean("screenshot_unlock_minimum_crop_limit"));
        initHook(SaveToPictures.INSTANCE, mPrefsMap.getBoolean("screenshot_save_to_pictures"));
        initHook(DeviceShellCustomize.INSTANCE, !TextUtils.isEmpty(mPrefsMap.getString("screenshot_device_customize", "")));
        initHook(UnlockPrivacyMarking.INSTANCE, mPrefsMap.getBoolean("screenshot_unlock_privacy_marking"));
        // 超级剪切板
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getStringAsInt("various_super_clipboard_e", 0) != 0);
    }
}
