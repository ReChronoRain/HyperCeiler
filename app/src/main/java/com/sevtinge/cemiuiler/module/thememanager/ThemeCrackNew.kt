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
import java.io.File

class ThemeCrackNew : BaseHook() {
    override fun init() {
        EzXHelperInit.setEzClassLoader(lpparam.classLoader)
        try {
            findAllMethods("com.android.thememanager.detail.theme.model.OnlineResourceDetail") {
                    name == "toResource"
                }.hookAfter {
                    it.thisObject.putObject("bought", true)
                }
                findAllMethods("com.android.thememanager.basemodule.views.DiscountPriceView") {
                    parameterCount == 2 && parameterTypes[0] == Int::class.javaPrimitiveType && parameterTypes[1] == Int::class.javaPrimitiveType
                }.hookBefore {
                    it.args[1] = 0
                }
                findMethod("com.miui.maml.widget.edit.MamlutilKt") {
                    name == "themeManagerSupportPaidWidget"
                }.hookAfter {
                    it.result = false
                }
                System.loadLibrary("dexkit")
                DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
                    val map = mapOf(
                        "DrmResult" to setOf("theme", "ThemeManagerTag", "/system"),
                        "LargeIcon" to setOf("apply failed", "/data/system/theme/large_icons/", "default_large_icon_product_id"),
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
                    val largeIcon = resultMap["LargeIcon"]!!
                    assert(largeIcon.size == 1)
                    val largeIconDescriptor = largeIcon.first()
                    val largeIconMethod: Method = largeIconDescriptor.getMethodInstance(lpparam.classLoader)
                    largeIconMethod.hookBefore {
                        val resource = findField(it.thisObject.javaClass) {
                            type == loadClass("com.android.thememanager.basemodule.resource.model.Resource", lpparam.classLoader)
                        }
                        val productId = it.thisObject.getObject(resource.name).invokeMethod("getProductId").toString()
                        File("/storage/emulated/0/Android/data/com.android.thememanager/files/MIUI/theme/.data/rights/theme/${productId}-largeicons.mra").createNewFile()
                    }
                }
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}
