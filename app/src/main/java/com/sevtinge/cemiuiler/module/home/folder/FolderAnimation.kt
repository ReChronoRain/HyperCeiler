package com.sevtinge.cemiuiler.module.home.folder

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.hookAfterMethod
import com.sevtinge.cemiuiler.utils.hookBeforeMethod
import de.robv.android.xposed.XC_MethodHook
import kotlin.math.abs

class FolderAnimation : BaseHook() {
    var mLauncher: Class<*>? = null
    private var value1: Float? = null
    private var value2: Float? = null
    private var value3: Float? = null
    private var value4: Float? = null

    override fun init() {//|x-200|    50-150
        value1 = abs(mPrefsMap.getInt("home_folder_anim_1", 90).toFloat() - 200) / 100
        value2 = mPrefsMap.getInt("home_folder_anim_2", 30).toFloat() / 100
        value3 = abs(mPrefsMap.getInt("home_folder_anim_3", 99).toFloat() - 200) / 100
        value4 = mPrefsMap.getInt("home_folder_anim_4", 24).toFloat() / 100
        val mSpringAnimator = findClassIfExists("com.miui.home.launcher.animate.SpringAnimator")
        var hook1: XC_MethodHook.Unhook? = null
        var hook2: XC_MethodHook.Unhook? = null

        for (i in 47..60) {
            val launcherClass = findClassIfExists("com.miui.home.launcher.Launcher$$i")
            if (launcherClass != null) {
                for (field in launcherClass.declaredFields) {
                    if (field.name == "val\$folderInfo") {
                        val mLauncherClass =
                            loadClassOrNull("com.miui.home.launcher.Launcher$$i")
                        if (mLauncherClass != null) {
                            for (field in mLauncherClass.declaredFields) {
                                if (field.name == "val\$folderInfo") {
                                    mLauncherClass.methodFinder().first {
                                        name == "run"
                                    }.createHook {
                                        before {
                                            hook1 = mSpringAnimator.methodFinder().first {
                                                name == "setDampingResponse"
                                                        && parameterTypes[0] == Float::class.javaPrimitiveType
                                                        && parameterTypes[1] == Float::class.javaPrimitiveType
                                            }.createHook {
                                                before {
                                                    it.args[0] = value1
                                                    it.args[1] = value2
                                                }
                                            }
                                        }
                                        after {
                                            hook1?.unhook()
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }

        "com.miui.home.launcher.Launcher".hookBeforeMethod("closeFolder", Boolean::class.java) {
            if (it.args[0] == true) {
                hook2 = mSpringAnimator.hookBeforeMethod(
                    "setDampingResponse", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType
                ) { hookParam ->
                    hookParam.args[0] = value3
                    hookParam.args[1] = value4
                }
            }
        }
        "com.miui.home.launcher.Launcher".hookAfterMethod("closeFolder", Boolean::class.java) {
            hook2?.unhook()
        }

    }
}
