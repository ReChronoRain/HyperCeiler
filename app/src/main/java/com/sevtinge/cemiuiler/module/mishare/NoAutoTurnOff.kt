package com.sevtinge.cemiuiler.module.mishare

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.enums.MatchType
import java.lang.reflect.Method

class NoAutoTurnOff : BaseHook() {

    @Throws(NoSuchMethodException::class)
    override fun init() {
        System.loadLibrary("dexkit")
        DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
            listOf("EnabledState").forEach { usingString ->
                val resultList = bridge.findMethodUsingString {
                    this.usingString = usingString
                    matchType = MatchType.SIMILAR_REGEX
                }
                assert(resultList.size == 1)
                val enabledReturn = resultList.first()
                val enabledReturnMethod: Method =
                    enabledReturn.getMethodInstance(lpparam.classLoader) //getMethodInstance 使用需定位到方法
                    // Lcom/miui/mishare/connectivity/MiShareService$j$g;->a()V 小米互传 2.16.0 定位方法名
                try {
                    enabledReturnMethod.hookBefore {
                        it.result = true
                    }
                    XposedBridge.log("Cemiuiler: $enabledReturn")
                    XposedBridge.log("Cemiuiler: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService success!")
                } catch (e: Throwable) {
                    XposedBridge.log("Cemiuiler: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService failed!")
                    XposedBridge.log(e)
                }
            }
        }
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