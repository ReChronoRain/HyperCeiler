package com.sevtinge.cemiuiler.module.mediaeditor

import android.os.Build
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.mediaeditor.MediaEditorDexKit.mMediaEditorResultMethodsMap


object FilterManagerAll : BaseHook() {
    private lateinit var device: String

    override fun init() {
        val result = mMediaEditorResultMethodsMap["FilterManager"]!!
        val methodResult = result.filter { it.isMethod }.map { it.getMethodInstance(safeClassLoader) }.toTypedArray()
        try {
            MethodFinder.fromArray(methodResult).first {
                returnType == List::class.java || (parameterCount == 1 && parameterTypes[0] == Bundle::class.java)
            }.createHook {
                before {
                    if (!this@FilterManagerAll::device.isInitialized) {
                        device = Build.DEVICE
                    }
                    setStaticObject(loadClass("android.os.Build"), "DEVICE", "wayne")
                }
                after {
                    setStaticObject(loadClass("android.os.Build"), "DEVICE", device)
                }
            }
        } catch (_: Throwable) {
        }
    }
}