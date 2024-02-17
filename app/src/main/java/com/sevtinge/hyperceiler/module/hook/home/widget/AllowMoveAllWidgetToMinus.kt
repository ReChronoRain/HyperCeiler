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
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.getObjectFieldOrNull

object AllowMoveAllWidgetToMinus : BaseHook() {
    override fun init() {
        try {
            loadClass("com.miui.home.launcher.widget.MIUIWidgetHelper").methodFinder()
                .filterByName("canDragToPa")
                .filterByParamCount(2)
                .single().createHook {
                    before {
                        val dragInfo = it.args[1].callMethod("getDragInfo")
                        val i = dragInfo?.getObjectField("spanX")
                        val launcherCallbacks = it.args[0].callMethod("getLauncherCallbacks")
                        val dragController = it.args[0].callMethod("getDragController")
                        val isDraggingFromAssistant =
                            dragController?.callMethod("isDraggingFromAssistant") as Boolean
                        val isDraggingToAssistant =
                            dragController.callMethod("isDraggingToAssistant") as Boolean
                        it.result =
                            launcherCallbacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && i != 1
                    }
                }
        } catch (e: Exception) {
            loadClass("com.miui.home.launcher.Workspace").methodFinder()
                .filterByName("canDragToPa")
                .single().createHook {
                    before {
                        val currentDragObject =
                            it.thisObject.getObjectFieldOrNull("mDragController")
                                ?.callMethod("getCurrentDragObject")
                        val dragInfo = currentDragObject?.callMethod("getDragInfo")
                        val i = dragInfo?.getObjectField("spanX")
                        val launcherCallbacks = it.thisObject.getObjectFieldOrNull("mLauncher")
                            ?.callMethod("getLauncherCallbacks")
                        val isDraggingFromAssistant =
                            it.thisObject.getObjectFieldOrNull("mDragController")
                                ?.callMethod("isDraggingFromAssistant") as Boolean
                        val isDraggingToAssistant =
                            it.thisObject.getObjectFieldOrNull("mDragController")
                                ?.callMethod("isDraggingToAssistant") as Boolean

                        it.result =
                            launcherCallbacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && i != 1
                    }
                }
        }

    }
}
