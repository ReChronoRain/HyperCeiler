package com.sevtinge.cemiuiler.module.mediaeditor

import android.os.Bundle
import com.github.kyuubiran.ezxhelper.utils.field
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.mediaeditor.MediaEditorDexKit.mMediaEditorResultMethodsMap
import java.lang.reflect.Method
import java.util.Objects

object FilterManagerAll : BaseHook() {
    override fun init() {
        try {
            val result = mMediaEditorResultMethodsMap["FilterManager"]!!
            for (descriptor in result) {
                try {
                    //val filterManager: Method = descriptor.getMethodInstance(lpparam.classLoader)
                    log("descriptor method is $descriptor")
                    val filterManager = descriptor.getMethodInstance(lpparam.classLoader)
                    log("filterManager method is $filterManager")
                    if (filterManager.returnType == List::class.java || filterManager.returnType == Bundle::class.java) {
                        filterManager.hookBefore {
                            loadClass("android.os.Build").field("DEVICE", true, String::class.java)
                                .set(null, "wayne")
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (e: Throwable) {
            logE(e)
        }
    }
}