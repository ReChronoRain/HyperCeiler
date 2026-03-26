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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.all

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isHyperOSVersion
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object SwapWiFiAndMobileNetwork : BaseHook() {
    val mSignalIcons by lazy {
        if (isHyperOSVersion(3f)) {
            arrayOf("wifi", "demo_wifi", "mobile", "demo_mobile", "no_sim")
        } else {
            arrayOf("hotspot", "slave_wifi", "wifi", "demo_wifi", "no_sim", "mobile", "demo_mobile", "airplane")
        }
    }

    override fun init() {
        val statusBarIconListClass =
            loadClass("com.android.systemui.statusbar.phone.ui.StatusBarIconList")

        statusBarIconListClass.constructorFinder()
            .filterByParamTypes { it[0] == Array<String>::class.java }
            .first()
            .createBeforeHook { param ->
                val isRightController =
                    "StatusBarIconList" == param.thisObject.javaClass.simpleName
                val allStatusIcons: ArrayList<Any?> = ArrayList((param.args[0] as Array<*>).toList())
                if (isRightController) {
                    var startIndex = allStatusIcons.indexOf("mobile")
                    val endIndex = allStatusIcons.indexOf("demo_wifi") + 1
                    val removedIcons = allStatusIcons.subList(startIndex, endIndex)
                    val mSignalRelatedIcons = ArrayList(listOf(*mSignalIcons))

                    removedIcons.clear()
                    startIndex = if (isHyperOSVersion(3f)) {
                        maxOf(allStatusIcons.indexOf("network_speed"), allStatusIcons.indexOf("hd"))
                    } else {
                        allStatusIcons.indexOf("hd")
                    }
                    allStatusIcons.addAll(startIndex + 1, mSignalRelatedIcons)
                    param.args[0] = allStatusIcons.map { it as String }.toTypedArray()
                }
            }
    }
}
