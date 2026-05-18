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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.display

import android.content.pm.ApplicationInfo
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isInternational
import com.sevtinge.hyperceiler.libhook.utils.hookapi.LazyClass.clazzMiuiBuild
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createInterceptHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setStaticBooleanField

// from SetoHook by SetoSkins
class AllDarkMode : BaseHook() {
    override fun init() {
        if (isInternational()) return
        val clazzForceDarkAppListManager =
            findClass("com.android.server.ForceDarkAppListManager")

        clazzForceDarkAppListManager.findAllMethods { name("getDarkModeAppList") }
            .forEach {
                it.createInterceptHook { chain ->
                    clazzMiuiBuild.setStaticBooleanField("IS_INTERNATIONAL_BUILD", true)
                    try {
                        chain.proceed()
                    } finally {
                        clazzMiuiBuild.setStaticBooleanField("IS_INTERNATIONAL_BUILD", false)
                    }
                }
            }

        clazzForceDarkAppListManager.findAllMethods { name("shouldShowInSettings") }
            .createHooks {
                before { param ->
                    val info = param.args[0] as ApplicationInfo?
                    param.result =
                        !(info == null || (
                            callMethod(info, "isSystemApp") as Boolean) || info.uid < 10000)
                }
            }
    }
}
