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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework.volume;

import android.media.AudioManager;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class VolumeFirstPress extends BaseHook {

    Class<?> mVolumeController;

    @Override
    public void init() {
        mVolumeController = findClassIfExists("com.android.server.audio.AudioService$VolumeController");

        findAndHookMethod(mVolumeController, "suppressAdjustment", int.class, int.class, boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                int streamType = (int) param.args[0];
                if (streamType != AudioManager.STREAM_MUSIC) return;
                boolean isMuteAdjust = (boolean) param.args[2];
                if (isMuteAdjust) return;
                Object mController = XposedHelpers.getObjectField(param.thisObject, "mController");
                if (mController == null) return;
                param.setResult(false);
            }
        });
    }
}
