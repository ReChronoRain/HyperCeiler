package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder

object NewSuperVolume {


    @JvmStatic
    fun initSuperVolume(classLoader: ClassLoader) {
        // 注：只显示 UI ，没提示，要提示等后续有缘人解决（
        val newVolumeClazz by lazy {
            loadClass("miui.systemui.util.VolumeUtils", classLoader)
        }
        val oldVolumeClazz by lazy {
            loadClass("miui.systemui.util.CommonUtils", classLoader)
        }

        try {
            newVolumeClazz.methodFinder().filterByName("getSUPER_VOLUME_PERCENT").first().createHook {
                returnConstant(200)
            }
            newVolumeClazz.methodFinder().filterByName("getSUPER_VOLUME_SUPPORTED").first().createHook {
                returnConstant(true)
            }
            newVolumeClazz.methodFinder().filterByName("getSUPER_VOLUME_VOICE_CALL_SUPPORTED").first().createHook {
                returnConstant(true)
            }
        } catch (t: Throwable) {
            oldVolumeClazz.methodFinder().filterByName("supportSuperVolume").first().createHook {
                returnConstant(true)
            }
           oldVolumeClazz.methodFinder().filterByName("voiceSupportSuperVolume").first().createHook {
                returnConstant(true)
            }
        }
    }
}