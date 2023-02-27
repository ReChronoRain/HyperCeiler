package com.sevtinge.cemiuiler.module.thememanager

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.luckypray.dexkit.DexKitBridge
import miui.drm.DrmManager
import java.lang.reflect.Method

class ThemeCrackNew : BaseHook() {
    override fun init() {
        EzXHelperInit.setEzClassLoader(lpparam.classLoader)
        try {
            System.loadLibrary("dexkit")
            DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
                val map = mapOf(
                    "DrmResult" to setOf("theme", "ThemeManagerTag", "/system"),
                )
                val resultMap = bridge.batchFindMethodsUsingStrings {
                    queryMap(map)
                }
                val drmResult = resultMap["DrmResult"]!!
                assert(drmResult.size == 1)
                val drmResultDescriptor = drmResult.first()
                val drmResultMethod: Method = drmResultDescriptor.getMethodInstance(lpparam.classLoader)
                drmResultMethod.hookAfter {
                    it.result = DrmManager.DrmResult.DRM_SUCCESS
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
        try {
            findAllMethods("com.android.thememanager.detail.theme.model.OnlineResourceDetail") {
                name == "toResource"
            }.hookAfter {
                it.thisObject.putObject("bought", true)
            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
        try {
            findAllMethods("com.android.thememanager.basemodule.views.DiscountPriceView") {
                parameterCount == 2 && parameterTypes[0] == Int::class.javaPrimitiveType && parameterTypes[1] == Int::class.javaPrimitiveType
            }.hookBefore {
                it.args[0] = 0
                it.args[1] = 0
            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
        try {
        findAndHookConstructor(
            "com.android.thememanager.detail.theme.model.OnlineResourceDetail",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    XposedHelpers.setBooleanField(param.thisObject, "bought", true)
                }
            })
        } catch (t: Throwable) {
            Log.ex(t)
        }
        try {
        findAndHookConstructor("com.android.thememanager.model.LargeIconElement", object : MethodHook() {
            @Throws(Throwable::class)
            override fun after(param: MethodHookParam) {
                XposedHelpers.setBooleanField(param.thisObject, "hasBought", true)
            }
        })
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}