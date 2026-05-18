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
import io.github.lingqiqi5211.ezhooktool.core.callMethodAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.replaceHookMethod as replaceMethod

object HideCollpasedFootButton {
    fun initLoaderHook(classLoader: ClassLoader) {

        loadClass(
            "com.android.systemui.miui.volume.MiuiVolumeDialogView",
            classLoader
        ).replaceMethod("updateFooterVisibility", Boolean::class.java) { param ->
            val thisObj = param.thisObjectOrNull ?: return@replaceMethod null
            val mRingerModeLayout = thisObj.getObjectFieldAs<View>("mRingerModeLayout")
            val mExpandButton = thisObj.getObjectFieldAs<View>("mExpandButton")
            val mExpanded = thisObj.callMethodAs<Boolean>("isExpanded")
            if (mExpanded) {
                mRingerModeLayout.visibility = View.VISIBLE
                mExpandButton.visibility = View.GONE
            } else {
                mRingerModeLayout.visibility = View.GONE
                mExpandButton.visibility = if (param.args[0] as Boolean) View.VISIBLE else View.GONE
            }
        }

    }
}
