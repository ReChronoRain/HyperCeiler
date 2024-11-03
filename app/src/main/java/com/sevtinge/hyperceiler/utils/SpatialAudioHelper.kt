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
package com.sevtinge.hyperceiler.utils

import android.content.*
import android.media.*
import com.github.kyuubiran.ezxhelper.*

object SpatialAudioHelper {
    @JvmStatic
    fun setSpatialAudioEnabled(context: Context, enabled: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val spatializer = AudioManager::class.java.getMethod("getSpatializer")
                .invoke(audioManager) as Spatializer
            Spatializer::class.java.getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                .invoke(spatializer, enabled)
        } catch (e: Exception) {
            Log.e("SpatialAudioHelper: Failed to set spatial audio state", e)
        }
    }

    @JvmStatic
    fun isSpatialAudioEnabled(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val spatializer = AudioManager::class.java.getMethod("getSpatializer")
            .invoke(audioManager) as Spatializer
        val isSpatialAudioEnabled =
            Spatializer::class.java.getMethod("isEnabled").invoke(spatializer) as Boolean
        return isSpatialAudioEnabled
    }
}