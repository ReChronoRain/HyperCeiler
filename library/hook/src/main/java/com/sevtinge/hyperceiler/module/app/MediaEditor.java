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
package com.sevtinge.hyperceiler.module.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.CustomWatermark;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockAigc;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockCustomPhotoFrames;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockDisney;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockLeicaFilter;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockMinimumCropLimit2;

import java.util.Objects;

@HookBase(targetPackage = "com.miui.mediaeditor")
public class MediaEditor extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // AI
        initHook(new UnlockAigc(), mPrefsMap.getBoolean("mediaeditor_unlock_aigc"));
        // 基础
        initHook(UnlockMinimumCropLimit2.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_minimum_crop_limit"));
        initHook(UnlockLeicaFilter.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_leica_filter"));
        initHook(CustomWatermark.INSTANCE, !Objects.equals(mPrefsMap.getString("mediaeditor_custom_watermark", ""), ""));
        // 创作
        if (mPrefsMap.getStringAsInt("mediaeditor_hook_type", 0) == 1) {
            initHook(UnlockCustomPhotoFrames.INSTANCE, mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0) != 0);
            initHook(UnlockDisney.INSTANCE, mPrefsMap.getStringAsInt("mediaeditor_unlock_disney_some_func", 0) != 0);
        } else if (mPrefsMap.getStringAsInt("mediaeditor_hook_type", 0) == 2) {
            initHook(UnlockCustomPhotoFrames.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_custom_photo_frames_v2"));
            initHook(UnlockDisney.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_disney_some_func_v2"));
        }
    }

}
