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
package com.sevtinge.hyperceiler.hook.module.hook.home

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.*
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils

abstract class HomeBaseHook : BaseHook() {
    private val isNewHome by lazy {
        // 最低版本未知
        getPackageVersionCode(lpparam) >= 539309777
    }

    override fun init() {
        if (isNewHome) {
            initForNewHome()
        } else {
            initForHomeLower9777()
        }
    }

    open fun initForNewHome() {}

    open fun initForHomeLower9777() {}

    @JvmOverloads
    protected fun setDimensionPixelSizeFormPrefs(key: String, defaultValue: Int = 0): MethodHook {
        return object : HookTool.MethodHook() {
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
