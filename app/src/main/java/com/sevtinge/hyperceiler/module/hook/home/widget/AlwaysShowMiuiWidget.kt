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
package com.sevtinge.hyperceiler.module.hook.home.widget

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.setObjectField
import de.robv.android.xposed.XC_MethodHook

object AlwaysShowMiuiWidget : BaseHook() {
    override fun init() {
        var hook1: XC_MethodHook.Unhook? = null
        var hook2: XC_MethodHook.Unhook? = null
        try {
            loadClass("com.miui.home.launcher.widget.WidgetsVerticalAdapter").methodFinder().first {
                name == "buildAppWidgetsItems"
            }
        } catch (e: Exception) {
            loadClass("com.miui.home.launcher.widget.BaseWidgetsVerticalAdapter").methodFinder().first {
                name == "buildAppWidgetsItems"
            }
        }.createHook {
            before {
                hook1 = loadClass("com.miui.home.launcher.widget.MIUIAppWidgetInfo").methodFinder()
                    .first {
                        name == "initMiuiAttribute" && parameterCount == 1
                    }.createHook {
                        after {
                            it.thisObject.setObjectField("isMIUIWidget", false)
                        }
                    }
                hook2 = loadClass("com.miui.home.launcher.MIUIWidgetUtil").methodFinder().first {
                    name == "isMIUIWidgetSupport"
                }.createHook {
                    after {
                        it.result = false
                    }
                }
            }
            after {
                hook1?.unhook()
                hook2?.unhook()
            }
        }
    }
}
