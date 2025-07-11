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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media

import android.util.TypedValue
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.helper.ObjectHelper.`-Static`.objectHelper
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

class MediaControlPanelTimeViewTextSize : BaseHook() {

    private val textSize by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_time_view_text_size", 13).toFloat()
    }

    //from https://github.com/YuKongA/MediaControl-BlurBg
    override fun init() {
        val miuiMediaControlPanel =
            loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")

        miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()
            ?.createAfterHook {
                val mMediaViewHolder =
                    it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder")
                        ?: return@createAfterHook
                val elapsedTimeView =
                    mMediaViewHolder.getObjectFieldOrNullAs<TextView>("elapsedTimeView")
                val totalTimeView =
                    mMediaViewHolder.getObjectFieldOrNullAs<TextView>("totalTimeView")

                elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
            }
    }
}
