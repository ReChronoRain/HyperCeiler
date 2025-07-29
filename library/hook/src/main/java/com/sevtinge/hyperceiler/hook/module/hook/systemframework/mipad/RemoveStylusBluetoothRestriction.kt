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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework.mipad

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.invokeStaticMethodBestMatch
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ObjectUtil.invokeMethodBestMatch
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks

object RemoveStylusBluetoothRestriction : BaseHook() {
    override fun init() {
        val clazzMiuiStylusDeviceListener =
            loadClass("com.miui.server.input.stylus.MiuiStylusDeviceListener")
        clazzMiuiStylusDeviceListener.declaredConstructors.createHooks {
            after {
                setTouchModeStylusEnable()
            }
        }
        clazzMiuiStylusDeviceListener.declaredMethods.createHooks {
            replace {
                setTouchModeStylusEnable()
            }
        }
    }

    private fun setTouchModeStylusEnable() {
        val driverVersion =
            mPrefsMap.getStringAsInt("mipad_input_bluetooth_version", 2)
        val flag: Int = 0x10 or driverVersion
        val instanceITouchFeature =
            invokeStaticMethodBestMatch(
                loadClass("miui.util.ITouchFeature"),
                "getInstance"
            )!!
        invokeMethodBestMatch(
            instanceITouchFeature,
            "setTouchMode",
            null,
            0, 20, flag
        )
    }
}
