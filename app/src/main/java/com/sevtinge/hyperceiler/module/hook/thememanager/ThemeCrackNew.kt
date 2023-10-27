package com.sevtinge.hyperceiler.module.hook.thememanager

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseXposedInit.mPrefsMap
import com.sevtinge.hyperceiler.utils.BuildUtils.getBuildType
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.closeDexKit
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.DexKit.initDexKit
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
import com.sevtinge.hyperceiler.utils.setObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage
import miui.drm.DrmManager
import java.io.File

class ThemeCrackNew {
    val TAG = "ThemeCrackNew"
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!mPrefsMap.getBoolean("various_enable_super_function") && !mPrefsMap.getBoolean("various_theme_crack") && !(getBuildType().equals("debug"))) return
        initDexKit(lpparam)
        try {
            loadClass("com.android.thememanager.detail.theme.model.OnlineResourceDetail").methodFinder().filterByName("toResource").toList().createHooks {
                after {
                    it.thisObject.setObjectField("bought", true)
                }
            }
        } catch (t: Throwable) {
            XposedLogUtils.logE(TAG, t)
        }
        try {
            loadClass("com.android.thememanager.basemodule.views.DiscountPriceView").methodFinder().filterByParamCount(2)
                .filterByParamTypes(Int::class.java, Int::class.java).filterByReturnType(Void.TYPE).toList().createHooks {
                    before {
                        it.args[1] = 0
                    }
                }
        } catch (t: Throwable) {
            XposedLogUtils.logE(TAG, t)
        }
        try {
            loadClass("com.android.thememanager.recommend.view.listview.viewholder.LargeIconDetailRecommendViewHolder").methodFinder().filterByParamCount(1)
                .filterByParamTypes(Int::class.java).filterByReturnType(Void.TYPE).toList().createHooks {
                    before {
                        it.args[0] = 0
                    }
                }
        } catch (t: Throwable) {
            XposedLogUtils.logE(TAG, t)
        }
        try {
            loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().filterByName("themeManagerSupportPaidWidget").first().createHook {
                returnConstant(false)
            }
        } catch (t: Throwable) {
            XposedLogUtils.logE(TAG, t)
        }

        try {
            dexKitBridge.findMethod {
                matcher {
                    addUsingStringsEquals("theme", "ThemeManagerTag", "/system", "check rights isLegal:")
                }
            }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
                after {
                    it.result = DrmManager.DrmResult.DRM_SUCCESS
                }
            }

            dexKitBridge.findMethod {
                matcher {
                    addUsingStringsEquals(
                        "apply failed",
                        "/data/system/theme/large_icons/",
                        "default_large_icon_product_id",
                        "largeicons",
                        "relativePackageList is empty"
                    )
                }
            }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
                before {
                    val resource = it.thisObject.javaClass.fieldFinder().filterByType(loadClass("com.android.thememanager.basemodule.resource.model.Resource", lpparam.classLoader)).first()
                    val productId = it.thisObject.objectHelper().getObjectOrNull(resource.name)!!.objectHelper().invokeMethodBestMatch("getProductId").toString()
                    val strPath = "/storage/emulated/0/Android/data/com.android.thememanager/files/MIUI/theme/.data/rights/theme/${productId}-largeicons.mra"
                    val file = File(strPath)
                    val fileParent = file.parentFile!!
                    if (!fileParent.exists()) fileParent.mkdirs()
                    file.createNewFile()
                }
            }
            closeDexKit()
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}
