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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.b

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.u.MediaPicture.optPicture
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object MediaPicture : BaseHook() {
    private val albumPictureCorners by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_media_control_album_picture_rounded_corners")
    }
    private val mode by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_media_album_mode", 0)
    }

    override fun init() {
        miuiMediaViewControllerImpl!!.methodFinder().filterByName("bindMediaData")
            .first().createAfterHook {
                val context =
                    it.thisObject.getObjectFieldOrNullAs<Context>("context")
                        ?: return@createAfterHook
                val mMediaViewHolder =
                    it.thisObject.getObjectFieldOrNull("holder")
                        ?: return@createAfterHook

                if (mode == 1) {
                    val appIcon =
                        mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("appIcon")
                    (appIcon?.parent as ViewGroup?)?.removeView(appIcon)
                }

                if (albumPictureCorners && mode != 2) {
                    optPicture(mMediaViewHolder, it, context)
                }
            }
    }
}
