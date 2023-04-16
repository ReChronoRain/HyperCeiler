package com.sevtinge.cemiuiler.module.mediaeditor

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.field
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

object FilterManagerAll : BaseHook() {
    override fun init() {

        XposedBridge.log("Cemiuiler: Rules Hook success!")
        // 1.0.3.2.1
//        "b6.b".hookBeforeMethod(
//            getDefaultClassLoader(), "g"
//        ) {
//            val field = findField("android.os.Build") { type == String::class.java && name == "DEVICE" }
//            it.thisObject.putObject(field, "wayne")
//            XposedBridge.log("Cemiuiler: HookBeforeMethod Hook success!")
//        }
        findMethod("b6.b") {
            name == "g"
        }.hookBefore {
//            param.thisObject.javaClass.field("DEVICE",true).setBoolean(param.thisObject, true)
            loadClass("android.os.Build").field("DEVICE", true, String::class.java)
                .set(null, "wayne")
            Log.ix("Cemiuiler: HookBeforeMethod Hook success!")
        }
    }
}