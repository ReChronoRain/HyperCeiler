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
package com.sevtinge.hyperceiler.libhook.app;

import android.text.TextUtils;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.screenshot.DeviceShellCustomize;
import com.sevtinge.hyperceiler.libhook.rules.screenshot.HideStatusBarWhenShot;
import com.sevtinge.hyperceiler.libhook.rules.screenshot.SaveToPictures;
import com.sevtinge.hyperceiler.libhook.rules.screenshot.UnlockCopyPicture;
import com.sevtinge.hyperceiler.libhook.rules.screenshot.UnlockMinimumCropLimit2;
import com.sevtinge.hyperceiler.libhook.rules.screenshot.UnlockPrivacyMarking;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.miui.screenshot")
public class ScreenShot extends BaseLoad {

    public ScreenShot() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        initHook(UnlockMinimumCropLimit2.INSTANCE, PrefsBridge.getBoolean("screenshot_unlock_minimum_crop_limit"));
        initHook(SaveToPictures.INSTANCE, PrefsBridge.getBoolean("screenshot_save_to_pictures"));
        initHook(DeviceShellCustomize.INSTANCE, !TextUtils.isEmpty(PrefsBridge.getString("screenshot_device_customize", "")));
        initHook(UnlockPrivacyMarking.INSTANCE, PrefsBridge.getBoolean("screenshot_unlock_privacy_marking"));
        initHook(UnlockCopyPicture.INSTANCE, PrefsBridge.getBoolean("screenshot_unlock_copy_to_clipboard"));
        initHook(HideStatusBarWhenShot.INSTANCE, PrefsBridge.getBoolean("system_ui_status_bar_hide_icon"));
    }
}
