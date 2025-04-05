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
package com.sevtinge.hyperceiler.module.hook.home.other

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.hookBeforeAllMethods

object BlurRadius : BaseHook() {
    override fun init() {

        val value = mPrefsMap.getInt("home_other_blur_radius", 100).toFloat() / 100
        if (value == 1f) return
        val blurUtilsClass = "com.miui.home.launcher.common.BlurUtils".findClass()
        blurUtilsClass.hookBeforeAllMethods("fastBlur") {
            it.args[0] = it.args[0] as Float * value
        }

    }
}
