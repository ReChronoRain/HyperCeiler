package com.voyager.star.hooks.rules.mediaeditor

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.field
import com.github.kyuubiran.ezxhelper.utils.findField
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.github.kyuubiran.ezxhelper.utils.putObject
import com.sevtinge.cemiuiler.module.base.BaseXposedInit.mPrefsMap
import com.voyager.star.utils.hasEnable
import com.voyager.star.utils.HookRegister
import com.voyager.star.utils.hookBeforeMethod
import de.robv.android.xposed.XposedBridge

object FilterManagerAll : HookRegister() {
    override fun init() {

        if (!mPrefsMap.getBoolean("mediaeditor_filter_manager"))
            return

        XposedBridge.log("Voyager-Test: Rules Hook success!")
        // 1.0.3.2.1
//        "b6.b".hookBeforeMethod(
//            getDefaultClassLoader(), "g"
//        ) {
//            val field = findField("android.os.Build") { type == String::class.java && name == "DEVICE" }
//            it.thisObject.putObject(field, "wayne")
//            XposedBridge.log("Voyager-Test: HookBeforeMethod Hook success!")
//        }
        findMethod("b6.b") {
            name == "g"
        }.hookBefore { param ->
//            param.thisObject.javaClass.field("DEVICE",true).setBoolean(param.thisObject, true)
            loadClass("android.os.Build").field("DEVICE", true, String::class.java)
                .set(null, "wayne")
            Log.ix("Voyager-Test: HookBeforeMethod Hook success!")
        }
    }
}