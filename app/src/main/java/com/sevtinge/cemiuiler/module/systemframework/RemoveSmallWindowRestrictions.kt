package com.sevtinge.cemiuiler.module.systemframework

import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook

object RemoveSmallWindowRestrictions : BaseHook() {
    override fun init() {
        // try {
        //     findAllMethods("android.provider.Settings\$Global") {
        //         name == "getInt"
        //     }.hookAfter { param ->
        //         XposedBridge.log("MaxFreeFormTest: android.provider.Settings\$Global.getInt called! param.args[1]: " + param.args[1])
        //         if (param.args[1] == "enable_non_resizable_multi_window") {
        //             val e = Throwable()
        //             XposedBridge.log(e)
        //             param.result = 1
        //         }
        //     }
        //     XposedBridge.log("Cemiuiler: Hook android.provider.Settings\$Global.getInt success!")
        // } catch (e: Throwable) {
        //     XposedBridge.log("Cemiuiler: Hook android.provider.Settings\$Global.getInt failed!")
        //     XposedBridge.log(e)
        // }
        //
        try {
            findAllMethods("com.android.server.wm.ActivityTaskManagerService") {
                name == "retrieveSettings"
            }.hookAfter { param ->
                param.thisObject.javaClass.field("mDevEnableNonResizableMultiWindow")
                    .setBoolean(param.thisObject, true)
            }
        } catch (e: Throwable) {
            log("Hook retrieveSettings failed by: $e")
        }

        try {
            findAllMethods("com.android.server.wm.WindowManagerService\$SettingsObserver") {
                name == "updateDevEnableNonResizableMultiWindow"
            }.hookAfter { param ->
                val this0 = param.thisObject.javaClass.field("this\$0").get(param.thisObject)
                val mAtmService = this0.javaClass.field("mAtmService").get(this0)
                mAtmService.javaClass.field("mDevEnableNonResizableMultiWindow").setBoolean(mAtmService,true)
            }
        } catch (e: Throwable) {
            log("Hook updateDevEnableNonResizableMultiWindow failed by: $e")
        }

        try {
            findAllMethods("com.android.server.wm.WindowManagerService\$SettingsObserver") {
                name == "onChange"
            }.hookAfter { param ->
                val this0 = param.thisObject.javaClass.field("this\$0").get(param.thisObject)
                val mAtmService = this0.javaClass.field("mAtmService").get(this0)
                mAtmService.javaClass.field("mDevEnableNonResizableMultiWindow").setBoolean(mAtmService,true)
            }
        } catch (e: Throwable) {
            log("Hook onChange failed by: $e")
        }

        try {
            findMethod("android.util.MiuiMultiWindowUtils") {
                name == "isForceResizeable"
            }.hookReturnConstant(true)
        } catch (e: Throwable) {
            log("Hook isForceResizeable failed by: $e")
        }

        // Author: LittleTurtle2333
        try {
            findMethod("com.android.server.wm.Task") {
                name == "isResizeable"
            }.hookReturnConstant(true)
        } catch (e: Throwable) {
            log("Hook isResizeable failed by: $e")
        }

        try {
            findMethod("android.util.MiuiMultiWindowAdapter") {
                name == "getFreeformBlackList"
            }.hookReturnConstant(mutableListOf<String>())
        } catch (e: Throwable) {
            log("Hook getFreeformBlackList failed by: $e")
        }

        try {
            findMethod("android.util.MiuiMultiWindowAdapter") {
                name == "getFreeformBlackListFromCloud" && parameterTypes[0] == Context::class.java
            }.hookReturnConstant(mutableListOf<String>())
        } catch (e: Throwable) {
            log("Hook getFreeformBlackListFromCloud failed by: $e")
        }

        try {
            findAllMethods("android.util.MiuiMultiWindowAdapter") {
                name == "getStartFromFreeformBlackListFromCloud"
            }.hookReturnConstant(mutableListOf<String>())
        } catch (e: Throwable) {
            log("Hook getStartFromFreeformBlackListFromCloud failed by: $e")
        }

        try {
            findMethod("android.util.MiuiMultiWindowUtils") {
                name == "supportFreeform"
            }.hookReturnConstant(true)
        } catch (e: Throwable) {
            log("Hook supportFreeform failed by: $e")
        }

    }

}