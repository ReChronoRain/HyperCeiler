package com.sevtinge.cemiuiler.module.mishare

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedBridge

class NoAutoTurnOff : BaseHook() {

    override fun init() {
        when (val version = Helpers.getPackageVersionCode(lpparam)) {
            21500 -> {
                findMethod("com.miui.mishare.connectivity.MiShareService\$d\$g") {
                    name == "b"
                }.hookBefore {
                    it.result = null
                }
            }

            21600 -> {
                findMethod("com.miui.mishare.connectivity.MiShareService\$j\$g") {
                    name == "a"
                }.hookBefore {
                    it.result = null
                }
            }

            else ->  XposedBridge.log("Cemiuiler: Your MiShare version is $version, NoAutoTurnOff doesn't work")
        }
    }
    /*@Throws(NoSuchMethodException::class)
    override fun init() {
        val mAutoOff2 = mMiShareResultMethodsMap["MiShareAutoOff2"]!!
        assert(mAutoOff2.isNotEmpty())
        var mAutoOff2Descriptor = mAutoOff2[0]
        var mAutoOff2Method: Method = mAutoOff2Descriptor.getMethodInstance(lpparam.classLoader)
        if (mAutoOff2Method.returnType != ArrayList::class.java) {
            mAutoOff2Descriptor = mAutoOff2[1]
            mAutoOff2Method = mAutoOff2Descriptor.getMethodInstance(lpparam.classLoader)
        }
        mAutoOff2Method.hookBefore {
            it.result = null
        }*/
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
}