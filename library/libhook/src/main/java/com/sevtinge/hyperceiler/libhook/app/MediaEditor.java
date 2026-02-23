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

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.mediaeditor.CustomWatermark;
import com.sevtinge.hyperceiler.libhook.rules.mediaeditor.UnlockAigc;
import com.sevtinge.hyperceiler.libhook.rules.mediaeditor.UnlockCustomPhotoFrames;
import com.sevtinge.hyperceiler.libhook.rules.mediaeditor.UnlockDisney;
import com.sevtinge.hyperceiler.libhook.rules.mediaeditor.UnlockLeicaFilter;
import com.sevtinge.hyperceiler.libhook.rules.mediaeditor.UnlockMinimumCropLimit2;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.util.Objects;

@HookBase(targetPackage = "com.miui.mediaeditor")
public class MediaEditor extends BaseLoad {

    public MediaEditor() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        // AI
        initHook(new UnlockAigc(), PrefsBridge.getBoolean("mediaeditor_unlock_aigc"));
        // 基础
        initHook(UnlockMinimumCropLimit2.INSTANCE, PrefsBridge.getBoolean("mediaeditor_unlock_minimum_crop_limit"));
        initHook(UnlockLeicaFilter.INSTANCE, PrefsBridge.getBoolean("mediaeditor_unlock_leica_filter"));
        initHook(CustomWatermark.INSTANCE, !Objects.equals(PrefsBridge.getString("mediaeditor_custom_watermark", ""), ""));
        // 创作
        initHook(UnlockCustomPhotoFrames.INSTANCE, PrefsBridge.getBoolean("mediaeditor_unlock_custom_photo_frames_v2"));
        initHook(UnlockDisney.INSTANCE, PrefsBridge.getBoolean("mediaeditor_unlock_disney_some_func_v2"));
    }

}
