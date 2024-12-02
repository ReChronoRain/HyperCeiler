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

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.ForceSupportPlaybackCapture;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.SaveToMovies;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.ScreenRecorderConfig;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.UnlockMoreVolumeFromNew;

@HookBase(targetPackage = "com.miui.screenrecorder",  isPad = false)
public class ScreenRecorder extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new ForceSupportPlaybackCapture(), mPrefsMap.getBoolean("screenrecorder_force_support_playback_capture"));
        initHook(UnlockMoreVolumeFromNew.INSTANCE, mPrefsMap.getBoolean("screenrecorder_more_volume"));
        initHook(new ScreenRecorderConfig(), mPrefsMap.getBoolean("screenrecorder_config"));
        initHook(SaveToMovies.INSTANCE, mPrefsMap.getBoolean("screenrecorder_save_to_movies"));
    }
}
