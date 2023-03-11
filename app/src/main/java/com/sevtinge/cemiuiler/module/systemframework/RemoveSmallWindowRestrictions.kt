package com.sevtinge.cemiuiler.module.systemframework

import android.content.ContentResolver
import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

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
            XposedBridge.log("Cemiuiler: Hook retrieveSettings success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook retrieveSettings failed!")
            XposedBridge.log(e)
        }

        try {
            findAllMethods("com.android.server.wm.WindowManagerService\$SettingsObserver") {
                name == "updateDevEnableNonResizableMultiWindow"
            }.hookAfter { param ->
                val this0 = param.thisObject.javaClass.field("this\$0").get(param.thisObject)
                val mAtmService = this0.javaClass.field("mAtmService").get(this0)
                mAtmService.javaClass.field("mDevEnableNonResizableMultiWindow").setBoolean(mAtmService,true)
            }
            XposedBridge.log("Cemiuiler: Hook updateDevEnableNonResizableMultiWindow success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook updateDevEnableNonResizableMultiWindow failed!")
            XposedBridge.log(e)
        }

        try {
            findAllMethods("com.android.server.wm.WindowManagerService\$SettingsObserver") {
                name == "onChange"
            }.hookAfter { param ->
                val this0 = param.thisObject.javaClass.field("this\$0").get(param.thisObject)
                val mAtmService = this0.javaClass.field("mAtmService").get(this0)
                mAtmService.javaClass.field("mDevEnableNonResizableMultiWindow").setBoolean(mAtmService,true)
            }
            XposedBridge.log("Cemiuiler: Hook WindowManagerService\$SettingsObserver.onChange success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook WindowManagerService\$SettingsObserver.onChange failed!")
            XposedBridge.log(e)
        }

        try {
            findMethod("android.util.MiuiMultiWindowUtils") {
                name == "isForceResizeable"
            }.hookReturnConstant(true)
            XposedBridge.log("Cemiuiler: Hook isForceResizeable success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook isForceResizeable failed!")
            XposedBridge.log(e)
        }

        // Author: LittleTurtle2333
        try {
            findMethod("com.android.server.wm.Task") {
                name == "isResizeable"
            }.hookReturnConstant(true)
            XposedBridge.log("Cemiuiler: Hook isResizeable success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook isResizeable failed!")
            XposedBridge.log(e)
        }

        try {
            findMethod("android.util.MiuiMultiWindowAdapter") {
                name == "getFreeformBlackList"
            }.hookReturnConstant(mutableListOf<String>())
            XposedBridge.log("Cemiuiler: Hook getFreeformBlackList success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook getFreeformBlackList failed!")
            XposedBridge.log(e)
        }

        try {
            findMethod("android.util.MiuiMultiWindowAdapter") {
                name == "getFreeformBlackListFromCloud" && parameterTypes[0] == Context::class.java
            }.hookReturnConstant(mutableListOf<String>())
            XposedBridge.log("Cemiuiler: Hook getFreeformBlackListFromCloud success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook getFreeformBlackListFromCloud failed!")
            XposedBridge.log(e)
        }

        try {
            findAllMethods("android.util.MiuiMultiWindowAdapter") {
                name == "getStartFromFreeformBlackListFromCloud"
            }.hookReturnConstant(mutableListOf<String>())
            XposedBridge.log("Cemiuiler: Hook getStartFromFreeformBlackListFromCloud success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook getStartFromFreeformBlackListFromCloud failed!")
            XposedBridge.log(e)
        }

        try {
            findMethod("android.util.MiuiMultiWindowUtils") {
                name == "supportFreeform"
            }.hookReturnConstant(true)
            XposedBridge.log("Cemiuiler: Hook supportFreeform success!")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: Hook supportFreeform failed!")
            XposedBridge.log(e)
        }

    }

}