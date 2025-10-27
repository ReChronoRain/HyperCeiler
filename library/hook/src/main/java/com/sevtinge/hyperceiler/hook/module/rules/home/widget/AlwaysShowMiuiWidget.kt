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
package com.sevtinge.hyperceiler.hook.module.rules.home.widget

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object AlwaysShowMiuiWidget : BaseHook() {
    private val isHookActive = object : ThreadLocal<Boolean>() {
        override fun initialValue(): Boolean = false
    }

    override fun init() {
        findBuildAppWidgetsItemsMethod().createHook {
            before {
                isHookActive.set(true)
            }
            after {
                isHookActive.remove()
            }
        }

        loadClass("com.miui.home.launcher.widget.MIUIAppWidgetInfo").methodFinder()
            .filterByName("initMiuiAttribute")
            .filterByParamCount(1)
            .single().createHook {
                before {
                    if (isHookActive.get() == true) {
                        it.thisObject.setObjectField("isMIUIWidget", false)
                    }
                }
            }

        loadClass("com.miui.home.launcher.MIUIWidgetUtil").methodFinder()
            .filterByName("isMIUIWidgetSupport")
            .single().createHook {
                before {
                    if (isHookActive.get() == true) {
                        it.result = false
                    }
                }
            }
    }

    private fun findBuildAppWidgetsItemsMethod() = try {
        loadClass("com.miui.home.launcher.widget.WidgetsVerticalAdapter").methodFinder()
            .filterByName("buildAppWidgetsItems")
            .single()
    } catch (_: Exception) {
        loadClass("com.miui.home.launcher.widget.BaseWidgetsVerticalAdapter").methodFinder()
            .filterByName("buildAppWidgetsItems")
            .single()
    }
}
