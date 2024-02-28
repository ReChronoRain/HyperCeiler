package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video

import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object UnlockVideoSomeFunc : BaseHook() {
    private val findFrc by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingString("ro.vendor.media.video.frc.support", StringMatchType.Equals)
                }
                returnType = "boolean"
                paramTypes("java.lang.String")
            }
        }
    }
    private val findTat by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingString("ro.vendor.media.video.frc.support", StringMatchType.Equals)
                }
                addUsingString("debug.config.media.video.ais.support", StringMatchType.Equals)
            }
        }.single().getMethodInstance(classLoader)
    }

    private val memc by lazy {
        // 动态画面补偿
        mPrefsMap.getBoolean("security_center_unlock_memc")
    }
    private val enhance by lazy {
        // 影像轮廓增强
        mPrefsMap.getBoolean("security_center_unlock_enhance_contours")
    }
   private val resolution by lazy {
        // 极清播放
        mPrefsMap.getBoolean("security_center_unlock_s_resolution")
   }

    override fun init() {
        val findFrcMethod = findFrc.filter { methodData ->
            methodData.usingFields.any {
                it.field.typeName == "java.util.List"
            }
        }
        val orderedA = findFrc.map { it.getMethodInstance(classLoader) }.toSet()
        val orderedB = findFrcMethod.map { it.getMethodInstance(classLoader) }.toSet()
        val differentItems = orderedA.subtract(orderedB)

        if (memc) {
            differentItems.forEach { methods ->
                logI(TAG, "find MeMc Method is $methods")
                hook(methods)
            }
        }

        var counter = 0
        orderedA.forEach { methods ->
            counter++
            if ((resolution || enhance) && counter == 1) {
                logI(TAG, "find Tat Method is $findTat")
            }

            if (counter == 1 && resolution) {
                logI(TAG, "find SuperResolution Method is $methods")

                hook(methods)
                hook(findTat)
            } else if (counter == 3 && enhance) {
                logI(TAG, "find EnhanceContours Method is $methods")
                hook(methods)

                val newChar = findTat.name.toCharArray()
                for (i in newChar.indices) {
                    newChar[i]++
                }
                val newName = String(newChar)
                findTat.declaringClass.methodFinder()
                    .filterByName(newName)
                    .first().createHook {
                        returnConstant(true)
                    }
            }
        }
    }

    private fun hook(methods: Method) {
        methods.createHook {
            returnConstant(true)
        }
    }
}
