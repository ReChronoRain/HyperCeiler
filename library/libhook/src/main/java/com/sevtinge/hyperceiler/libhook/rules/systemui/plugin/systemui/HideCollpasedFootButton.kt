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
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.view.View
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethodAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.replaceMethod
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass

object HideCollpasedFootButton {
    fun initLoaderHook(classLoader: ClassLoader) {

        loadClass(
            "com.android.systemui.miui.volume.MiuiVolumeDialogView",
            classLoader
        ).replaceMethod("updateFooterVisibility", Boolean::class.java) {
            val thisObj = it.thisObject
            val mRingerModeLayout = thisObj.getObjectFieldAs<View>("mRingerModeLayout")
            val mExpandButton = thisObj.getObjectFieldAs<View>("mExpandButton")
            val mExpanded = thisObj.callMethodAs<Boolean>("isExpanded")
            if (mExpanded) {
                mRingerModeLayout.visibility = View.VISIBLE
                mExpandButton.visibility = View.GONE
            } else {
                mRingerModeLayout.visibility = View.GONE
                mExpandButton.visibility = if (it.args[0] as Boolean) View.VISIBLE else View.GONE
            }
        }

    }
}
