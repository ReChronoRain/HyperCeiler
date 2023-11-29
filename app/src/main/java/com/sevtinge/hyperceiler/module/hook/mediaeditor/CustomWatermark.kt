package com.sevtinge.hyperceiler.module.hook.mediaeditor

import android.annotation.SuppressLint
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.utils.hook.LazyClass.SystemProperties

@SuppressLint("StaticFieldLeak")
object CustomWatermark : BaseHook() {

    override fun init(){
        SystemProperties.methodFinder()
            .filterByParamCount(2)
            .filterByParamTypes(String::class.java, String::class.java)
            .toList().createHooks {
                before {
                    if (it.args[0] == "ro.product.marketname") {
                        it.args[1] = mPrefsMap.getString("mediaeditor_custom_watermark", "")
                    }
                }
            }

        SystemProperties.methodFinder()
            .filterByName("get")
            .filterByParamTypes(String::class.java)
            .toList().createHooks {
                before {
                    if (it.args[0] == "ro.product.marketname") {
                        it.result = mPrefsMap.getString("mediaeditor_custom_watermark", "")
                    }
                }
            }
    }
}
