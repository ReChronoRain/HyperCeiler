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
package com.sevtinge.hyperceiler.libhook.rules.screenrecorder

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.beforeHookMethod

class ForceSupportPlaybackCapture : BaseHook() {
    override fun init() {
        // if (!xPrefs.getBoolean("force_support_playbackcapture", true)) return

        "android.os.SystemProperties".beforeHookMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.java
        ) { param ->
            val param0 = param.args[0] as String
            if (param0 == "ro.vendor.audio.playbackcapture.screen")
                param.result = true
        }
    }
}
