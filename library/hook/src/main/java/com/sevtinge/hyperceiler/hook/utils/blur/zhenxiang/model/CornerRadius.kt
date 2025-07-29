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
package com.sevtinge.hyperceiler.hook.utils.blur.zhenxiang.model

data class CornersRadius(
    val topLeft: Float,
    val topRight: Float,
    val bottomLeft: Float,
    val bottomRight: Float,
) {

    companion object {
        fun all(radius: Float): CornersRadius {
            return CornersRadius(radius, radius, radius, radius)
        }

        fun custom(
            topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float
        ): CornersRadius {
            return CornersRadius(topLeft, topRight, bottomLeft, bottomRight)
        }
    }
}
