package com.sevtinge.cemiuiler.module.thememanager

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.thememanager.ThemeManagerDexKit.mThemeManagerResultMethodsMap
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.getObjectField
import com.sevtinge.cemiuiler.utils.setObjectField
import miui.drm.DrmManager
import java.io.File

class ThemeCrackNew : BaseHook() {
    override fun init() {
        try {
            loadClass("com.android.thememanager.detail.theme.model.OnlineResourceDetail").methodFinder().filter {
                name == "toResource"
            }.toList().createHooks {
                after {
                    it.thisObject.setObjectField("bought", true)
                }
            }
            loadClass("com.android.thememanager.basemodule.views.DiscountPriceView").methodFinder().filter {
                parameterCount == 2 && parameterTypes[0] == Int::class.javaPrimitiveType && parameterTypes[1] == Int::class.javaPrimitiveType
            }.toList().createHooks {
                before {
                    it.args[0] = 0
                    it.args[1] = 0
                }
            }
            loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().first {
                name == "themeManagerSupportPaidWidget"
            }.createHook {
                after {
                    it.result = false
                }
            }
            val drmResult = mThemeManagerResultMethodsMap["DrmResult"]!!
            for (descriptor in drmResult) {
                try {
                    // val filterManager: Method = descriptor.getMethodInstance(lpparam.classLoader)
                    val drmResultMethod = descriptor.getMethodInstance(lpparam.classLoader)
                    log("DrmResult method is $drmResultMethod")
                    drmResultMethod.createHook {
                        after {
                            it.result = DrmManager.DrmResult.DRM_SUCCESS
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            /*val drmResult = mThemeManagerResultMethodsMap["DrmResult"]!!
            assert(drmResult.size == 1)
            val drmResultDescriptor = drmResult.first()
            val drmResultMethod: Method = drmResultDescriptor.getMethodInstance(lpparam.classLoader)
            log("DrmResult method is $drmResultMethod")
            drmResultMethod.hookAfter {
                it.result = DrmManager.DrmResult.DRM_SUCCESS
            }
             */
            val largeIcon = mThemeManagerResultMethodsMap["LargeIcon"]!!
            for (descriptor in largeIcon) {
                try {
                    // val filterManager: Method = descriptor.getMethodInstance(lpparam.classLoader)
                    val largeIconMethod = descriptor.getMethodInstance(lpparam.classLoader)
                    log("largeIcon method is $largeIconMethod")
                    largeIconMethod.createHook {
                        before {
                            largeIconMethod.createHook {
                                before {
                                    val resource = (it.thisObject.javaClass).fieldFinder().first {
                                        type == loadClass(
                                            "com.android.thememanager.basemodule.resource.model.Resource",
                                            lpparam.classLoader
                                        )
                                    }

                                    val productId = it.thisObject.getObjectField(resource.name)
                                        ?.callMethod("getProductId")
                                        .toString()
                                    val strPath =
                                        "/storage/emulated/0/Android/data/com.android.thememanager/files/MIUI/theme/.data/rights/theme/${productId}-largeicons.mra"
                                    val file = File(strPath)
                                    val fileParent = file.parentFile!!
                                    if (!fileParent.exists()) fileParent.mkdirs()
                                    file.createNewFile()
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            /*val largeIcon = mThemeManagerResultMethodsMap["LargeIcon"]!!
            assert(largeIcon.size == 1)
            val largeIconDescriptor = largeIcon.first()
            val largeIconMethod: Method = largeIconDescriptor.getMethodInstance(lpparam.classLoader)
            log("largeIcon method is $largeIconMethod")
            largeIconMethod.hookBefore {
                val resource = findField(it.thisObject.javaClass) {
                    type == loadClass(
                        "com.android.thememanager.basemodule.resource.model.Resource",
                        lpparam.classLoader
                    )
                }
                val productId =
                    it.thisObject.getObject(resource.name).invokeMethod("getProductId").toString()
                val strPath =
                    "/storage/emulated/0/Android/data/com.android.thememanager/files/MIUI/theme/.data/rights/theme/${productId}-largeicons.mra"
                val file = File(strPath)
                val fileParent = file.parentFile!!
                if (!fileParent.exists()) fileParent.mkdirs()
                file.createNewFile()

            }

             */
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}
