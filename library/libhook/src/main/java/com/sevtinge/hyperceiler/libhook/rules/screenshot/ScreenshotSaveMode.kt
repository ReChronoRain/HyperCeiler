/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.screenshot

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook

/**
 * Screenshot saving mode hook.
 * Controls whether the original screenshot is retained when edited (Keep both vs Replace screenshot).
 */
object ScreenshotSaveMode : BaseHook() {

    override fun init() {
        hookAllMethods(
            "com.miui.gallery.editor.photo.screen.helper.ScreenshotShareHelper",
            "deleteSourceAndExportAndFinish",
            object : IMethodHook {
                override fun before(param: HookParam) {
                    val args = param.args
                    if (args.size >= 5 && args[4] is Boolean) {
                        val sourceUri = args.getOrNull(2)
                        val exportUri = args.getOrNull(3)
                        if (sourceUri != null && exportUri != null && sourceUri != exportUri) {
                            args[4] = false
                            args.getOrNull(1)?.let { activity ->
                                setBooleanField(activity, "mIsExportedAndSourceDeleted", true)
                            }
                        }
                    }
                }
            }
        )
    }
}

