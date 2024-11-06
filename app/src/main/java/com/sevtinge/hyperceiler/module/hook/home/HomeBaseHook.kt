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
package com.sevtinge.hyperceiler.module.hook.home

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.*
import com.sevtinge.hyperceiler.utils.devicesdk.*

abstract class HomeBaseHook : BaseHook() {
    protected val isNewHome by lazy {
        // 最低版本未知
        getPackageVersionCode(lpparam) >= 539309777
    }

    @JvmOverloads
    protected fun setDimensionPixelSizeFormPrefs(key: String, defaultValue: Int = 0): MethodHook {
        return object : MethodHook() {
            override fun before(param: MethodHookParam) {
                param.result = DisplayUtils.dp2px(
                    mPrefsMap.getInt(key, defaultValue).toFloat()
                )
            }
        }
    }

    companion object {
        const val DEVICE_CONFIG = "com.miui.home.launcher.DeviceConfig"
        const val GRID_CONFIG = "com.miui.home.launcher.GridConfig"
    }
}