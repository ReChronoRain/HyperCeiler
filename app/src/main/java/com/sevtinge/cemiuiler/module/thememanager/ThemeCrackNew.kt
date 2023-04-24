package com.sevtinge.cemiuiler.module.thememanager

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.thememanager.ThemeManagerDexKit.mThemeManagerResultMethodsMap
import io.luckypray.dexkit.DexKitBridge
import miui.drm.DrmManager
import java.io.File
import java.lang.reflect.Method

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
            val drmResult = mThemeManagerResultMethodsMap["DrmResult"]!!
            assert(drmResult.size == 1)
            val drmResultDescriptor = drmResult.first()
            val drmResultMethod: Method = drmResultDescriptor.getMethodInstance(lpparam.classLoader)
            drmResultMethod.hookAfter {
                it.result = DrmManager.DrmResult.DRM_SUCCESS
            }
            val largeIcon = mThemeManagerResultMethodsMap["LargeIcon"]!!
            assert(largeIcon.size == 1)
            val largeIconDescriptor = largeIcon.first()
            val largeIconMethod: Method = largeIconDescriptor.getMethodInstance(lpparam.classLoader)
            largeIconMethod.hookBefore {
                val resource = findField(it.thisObject.javaClass) {
                    type == loadClass(
                        "com.android.thememanager.basemodule.resource.model.Resource",
                        lpparam.classLoader
                    )
                }
                val productId = it.thisObject.getObject(resource.name).invokeMethod("getProductId").toString()
                val strPath = "/storage/emulated/0/Android/data/com.android.thememanager/files/MIUI/theme/.data/rights/theme/${productId}-largeicons.mra"
                val file = File(strPath)
                val fileParent = file.parentFile!!
                if (!fileParent.exists()) fileParent.mkdirs()
                file.createNewFile()

            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}
