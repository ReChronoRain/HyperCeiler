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
package com.sevtinge.hyperceiler.module.hook.systemui;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class MediaSeekBarColor extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        int progressColor = mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_color", -1);
        int thumbColor = mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1);
        findAndHookMethod("com.android.systemui.media.controls.models.player.SeekBarObserver",
                "onChanged", Object.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Object holder = XposedHelpers.getObjectField(param.thisObject, "holder");
                        SeekBar seekBar = (SeekBar) XposedHelpers.getObjectField(holder, "seekBar");
                        if (progressColor != -1)
                            seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(progressColor, PorterDuff.Mode.SRC_IN));
                        if (thumbColor != -1)
                            seekBar.getThumb().setColorFilter(new PorterDuffColorFilter(thumbColor, PorterDuff.Mode.SRC_IN));
                    }
                }
        );
    }
}
