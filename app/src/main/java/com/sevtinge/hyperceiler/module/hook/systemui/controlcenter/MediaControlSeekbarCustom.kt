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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.content.res.Resources.*
import android.graphics.drawable.*
import android.util.Log
import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.sevtinge.hyperceiler.module.base.*

class MediaControlSeekbarCustom : BaseHook() {

    private val progressThickness by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_progress_thickness", 80)
    }

    //from https://github.com/YuKongA/MediaControl-BlurBg
    override fun init() {
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)

        val seekBarObserver =
            loadClassOrNull("com.android.systemui.media.controls.models.player.SeekBarObserver")

        seekBarObserver?.constructors?.createHooks {
            after {
                it.thisObject.objectHelper().setObject("seekBarEnabledMaxHeight", progressThickness.dp)
                it.args[0].objectHelper().getObjectOrNullAs<SeekBar>("seekBar")?.apply {
                    thumb = (thumb as Drawable).apply {
                        setMinimumWidth(progressThickness.dp)
                        setMinimumHeight(progressThickness.dp)
                    }
                }
            }
        }

    }

    val Int.dp: Int get() = (this.toFloat().dp).toInt()

    val Float.dp: Float get() = this / getSystem().displayMetrics.density
}
