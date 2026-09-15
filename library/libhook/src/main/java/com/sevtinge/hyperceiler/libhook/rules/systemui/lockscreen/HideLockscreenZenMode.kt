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
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object HideLockscreenZenMode : BaseHook() {
    private const val ZEN_MODE_VIEW_CONTROLLER =
        "com.android.systemui.statusbar.notification.zen.ZenModeViewController"

    override fun init() {
        // HyperOS 4.0 (Android 17) dropped ZenModeViewController from SystemUI entirely --
        // neither the class nor its manuallyDismissed field is left anywhere in
        // MiuiSystemUI.apk, and no replacement was found in the plugin either. Loading it
        // unconditionally therefore threw ClassNotFoundError on every SystemUI start, which
        // only filled the log: there is nothing left to hide on that release.
        val zenModeClass = findClassIfExists(ZEN_MODE_VIEW_CONTROLLER) ?: return

        // hyperOS fix by hyper helper
        zenModeClass.findMethod { filter { name.startsWith("updateVisibility") } }
            .createBeforeHook {
                it.thisObject.setObjectField("manuallyDismissed", true)
            }
    }
}
