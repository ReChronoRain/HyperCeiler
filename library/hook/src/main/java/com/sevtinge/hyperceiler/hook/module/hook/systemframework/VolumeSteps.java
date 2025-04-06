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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class VolumeSteps extends BaseHook {

    Class<?> mAudioService;

    @Override
    public void init() {

        mAudioService = findClass("com.android.server.audio.AudioService");

        findAndHookMethod(mAudioService, "createStreamStates", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {

                int[] maxStreamVolume = (int[]) XposedHelpers.getStaticObjectField(mAudioService, "MAX_STREAM_VOLUME");
                int mult = mPrefsMap.getInt("system_framework_volume_steps", 0);
                if (mult <= 0) return;
                for (int i = 0; i < maxStreamVolume.length; i++)
                    maxStreamVolume[i] = Math.round(maxStreamVolume[i] * mult / 100.0f);
                XposedHelpers.setStaticObjectField(mAudioService, "MAX_STREAM_VOLUME", maxStreamVolume);
            }
        });
    }
}
