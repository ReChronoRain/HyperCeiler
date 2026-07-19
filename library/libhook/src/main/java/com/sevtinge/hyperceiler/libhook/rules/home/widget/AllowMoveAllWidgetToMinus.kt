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
package com.sevtinge.hyperceiler.libhook.rules.home.widget

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setBooleanField

object AllowMoveAllWidgetToMinus : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initOS3Hook() {
        runCatching {
            loadClass("com.miui.home.launcher.widget.MIUIWidgetHelper").findMethod { name("canDragToPa"); paramCount(2) }.createHook {
                    before {
                        val dragInfo = it.args[1]!!.callMethod("getDragInfo")
                        dragInfo?.setBooleanField("isMIUIWidget", true)
                    }
                }
        }.onFailure {
            XposedLog.e(TAG, lpparam.packageName,  "init failed, ${it.message} callback OldHook Code")
        }
    }

    override fun initBase() {
        try {
            loadClass("com.miui.home.launcher.widget.MIUIWidgetHelper").findMethod { name("canDragToPa"); paramCount(2) }.createHook {
                    before {
                        val dragInfo = it.args[1]?.callMethod("getDragInfo")
                        val i = dragInfo?.getObjectField("spanX")
                        val launcherCallbacks = it.args[0]?.callMethod("getLauncherCallbacks")
                        val dragController = it.args[0]?.callMethod("getDragController")
                        val isDraggingFromAssistant =
                            dragController?.callMethod("isDraggingFromAssistant") as Boolean
                        val isDraggingToAssistant =
                            dragController.callMethod("isDraggingToAssistant") as Boolean
                        it.result =
                            launcherCallbacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && i != 1
                    }
                }
        } catch (_: Exception) {
            loadClass("com.miui.home.launcher.Workspace").findMethod { name("canDragToPa") }.createHook {
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
