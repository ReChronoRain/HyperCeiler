package com.sevtinge.cemiuiler.module.home.widget

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAuto
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod

object AllowMoveAllWidgetToMinus : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_widget_to_minus")) return
        try {
            findMethod("com.miui.home.launcher.widget.MIUIWidgetHelper") {
                name == "canDragToPa" && parameterCount == 2
            }.hookBefore {
                val dragInfo = it.args[1].invokeMethodAuto("getDragInfo")
                val i = dragInfo?.getObject("spanX")
                val launcherCallbacks = it.args[0].invokeMethodAuto("getLauncherCallbacks")
                val dragController = it.args[0].invokeMethodAuto("getDragController")
                val isDraggingFromAssistant = dragController?.callMethod("isDraggingFromAssistant") as Boolean
                val isDraggingToAssistant = dragController.callMethod("isDraggingToAssistant") as Boolean
                it.result = launcherCallbacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && i != 1
            }
        } catch (e: Exception) {
            findMethod("com.miui.home.launcher.Workspace") {
                name == "canDragToPa"
            }.hookBefore {
                val currentDragObject = it.thisObject.getObjectOrNull("mDragController")?.invokeMethodAuto("getCurrentDragObject")
                val dragInfo = currentDragObject?.invokeMethodAuto("getDragInfo")
                val i = dragInfo?.getObject("spanX")
                val launcherCallbacks = it.thisObject.getObjectOrNull("mLauncher")?.invokeMethodAuto("getLauncherCallbacks")
                val isDraggingFromAssistant = it.thisObject.getObjectOrNull("mDragController")?.callMethod("isDraggingFromAssistant") as Boolean
                val isDraggingToAssistant = it.thisObject.getObjectOrNull("mDragController")?.callMethod("isDraggingToAssistant") as Boolean
                it.result = launcherCallbacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && i != 1
            }
        }

    }
}


