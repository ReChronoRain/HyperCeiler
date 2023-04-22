package com.sevtinge.cemiuiler.module.mishare

import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge
import io.luckypray.dexkit.DexKitBridge

class NoAutoTurnOff : BaseHook() {

    @Throws(NoSuchMethodException::class)
    override fun init() {
        System.loadLibrary("dexkit")
        DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
            bridge.findMethodUsingString {
                usingString = "EnabledState"
                usingString = "MiShareService"
                usingString = "mishare_enabled"
            }.map {
                it.getMethodInstance(InitFields.ezXClassLoader)
            }.hookBefore {
                try {
                    it.result = null
                    XposedBridge.log("Cemiuiler: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService success!")
                } catch (e: Throwable) {
                    XposedBridge.log("Cemiuiler: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService failed!")
                    XposedBridge.log(e)
                }
            }
        }
//            listOf("MiShareService", "EnabledState").forEach { usingString ->
//                val resultList = bridge.findMethodUsingString {
//                    this.usingString = usingString
//                    matchType = MatchType.SIMILAR_REGEX
//                }
//                assert(resultList.size == 2)
//                val enabledReturn = resultList.first()
//                val enabledReturnMethod: Method =
//                    enabledReturn.getMethodInstance(lpparam.classLoader)
//                // Lcom/miui/mishare/connectivity/MiShareService$d$g;->b()V 小米互传 2.15.0 定位方法名
//                // Lcom/miui/mishare/connectivity/MiShareService$j$g;->a()V 小米互传 2.16.0 定位方法名
//
//            }

//        try {
//            findMethod("com.miui.mishare.connectivity.MiShareService\$d\$g") {
//                name == "b"
//            }.hookBefore {
//                it.result = null
//            }
//            XposedBridge.log("Cemiuiler: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService success!")
//        } catch (e: Throwable) {
//            XposedBridge.log("Cemiuiler: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService failed!")
//            XposedBridge.log(e)
//        }
//
    }
}