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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media

import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/data/MiuiMediaViewHolderWrapper.kt
data class MiuiMediaViewHolderWrapper(
    val innerHashCode: Int,
    val titleText: TextView,
    val artistText: TextView,
    val albumView: ImageView,
    val mediaBg: ImageView,
    val seamlessIcon: ImageView,
    val action0: ImageButton,
    val action1: ImageButton,
    val action2: ImageButton,
    val action3: ImageButton,
    val action4: ImageButton,
    val elapsedTimeView: TextView,
    val totalTimeView: TextView,
    val seekBar: SeekBar
)
