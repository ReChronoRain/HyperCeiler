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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media

import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.defaultColorConfig
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable.MediaControlBgDrawable

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/data/PlayerConfig.kt
data class PlayerConfig(
    var artworkBoundId: Int = 0,
    var artworkNextBindRequestId: Int = 0,
    var artworkDrawable: MediaControlBgDrawable? = null,
    var isArtworkBound: Boolean = false,
    var currentPkgName: String = "",
    var currColorConfig: MediaViewColorConfig = defaultColorConfig,
    var lastWidth: Int = 0,
    var lastHeight: Int = 0,
) {
    fun reset() {
        artworkDrawable = null
        isArtworkBound = false
        currentPkgName = ""
    }
}
