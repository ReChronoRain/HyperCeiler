package com.sevtinge.cemiuiler.module.home.widget

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.getObjectFieldOrNull

object AllowMoveAllWidgetToMinus : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_widget_to_minus")) return
        try {
            loadClass("com.miui.home.launcher.widget.MIUIWidgetHelper").methodFinder().first {
                name == "canDragToPa" && parameterCount == 2
            }.createHook {
                before {
                val dragInfo = it.args[1].invokeMethodAuto("getDragInfo")
                val i = dragInfo?.getObject("spanX")
                val launcherCallbacks = it.args[0].invokeMethodAuto("getLauncherCallbacks")
                val dragController = it.args[0].invokeMethodAuto("getDragController")
                val isDraggingFromAssistant = dragController?.callMethod("isDraggingFromAssistant") as Boolean
                val isDraggingToAssistant = dragController.callMethod("isDraggingToAssistant") as Boolean
                it.result = launcherCallbacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && i != 1
            }
            }
        } catch (e: Exception) {
            loadClass("com.miui.home.launcher.Workspace").methodFinder().first {
                name == "canDragToPa"
            }.createHook {
                before {
                val currentDragObject = it.thisObject.getObjectFieldOrNull("mDragController")?.invokeMethodAuto("getCurrentDragObject")
                val dragInfo = currentDragObject?.invokeMethodAuto("getDragInfo")
                val i = dragInfo?.getObject("spanX")
                val launcherCallbacks = it.thisObject.getObjectFieldOrNull("mLauncher")?.invokeMethodAuto("getLauncherCallbacks")
                val isDraggingFromAssistant = it.thisObject.getObjectFieldOrNull("mDragController")?.callMethod("isDraggingFromAssistant") as Boolean
                val isDraggingToAssistant = it.thisObject.getObjectFieldOrNull("mDragController")?.callMethod("isDraggingToAssistant") as Boolean
                it.result = launcherCallbacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && i != 1
            }
            }
        }

    }
}

