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
package com.sevtinge.hyperceiler.hook.module.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.camera.BlackLeica;
import com.sevtinge.hyperceiler.hook.module.hook.camera.CustomCameraColor;
import com.sevtinge.hyperceiler.hook.module.hook.camera.CustomWatermark;
import com.sevtinge.hyperceiler.hook.module.hook.camera.EnableLabOptions;
import com.sevtinge.hyperceiler.hook.module.hook.camera.MaxScreenBrightness;
import com.sevtinge.hyperceiler.hook.module.hook.camera.UnlockTrackEyes;

@HookBase(targetPackage = "com.android.camera")
public class Camera extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // 拍照
        initHook(new CustomWatermark(), mPrefsMap.getBoolean("camera_custom_watermark"));
        initHook(new BlackLeica(), mPrefsMap.getBoolean("camera_black_leica"));

        // 设置
        initHook(new EnableLabOptions(), mPrefsMap.getBoolean("camera_settings_lab_options"));
        initHook(new UnlockTrackEyes(), mPrefsMap.getBoolean("camera_settings_track_eyes"));

        initHook(new MaxScreenBrightness(), mPrefsMap.getBoolean("camera_max_brightness"));
        initHook(new CustomCameraColor(), mPrefsMap.getBoolean("camera_custom_theme_color"));
    }
}
