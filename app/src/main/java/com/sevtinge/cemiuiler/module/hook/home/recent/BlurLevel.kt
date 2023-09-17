package com.sevtinge.cemiuiler.module.hook.home.recent

import android.app.Activity
import android.view.MotionEvent
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.isPad
import com.sevtinge.cemiuiler.utils.callStaticMethod
import com.sevtinge.cemiuiler.utils.hookAfterAllMethods
import com.sevtinge.cemiuiler.utils.hookBeforeMethod

object BlurLevel : BaseHook() {
    private val blurLevel by lazy {
        mPrefsMap.getStringAsInt("home_recent_blur_level", 6)
    }

    override fun init() {
        val mBlurClass = loadClass("com.miui.home.launcher.common.BlurUtils")

        mBlurClass.methodFinder().first {
            name == "getBlurType"
        }.createHook {
            before {
                when (blurLevel) {
                    5 -> returnConstant(2)
                    0 -> returnConstant(2)
                    2 -> returnConstant(1)
                    3 -> returnConstant(0)
                    4 -> returnConstant(0)
                }

            }
        }

        when (blurLevel) {
            4 -> {
                mBlurClass.methodFinder().first {
                    name == "isUseCompleteBlurOnDev"
                }.createHook {
                    returnConstant(false)
                }

                "com.miui.home.launcher.common.DeviceLevelUtils".hookBeforeMethod("isUseSimpleAnim") {
                    it.result = true
                }
            }

            5 -> {
                val blurClass = findClassIfExists("com.miui.home.launcher.common.BlurUtils")
                val navStubViewClass = findClassIfExists("com.miui.home.recents.NavStubView")
                val applicationClass = findClassIfExists("com.miui.home.launcher.Application")

                navStubViewClass.hookBeforeMethod("onPointerEvent", MotionEvent::class.java) {
                    val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                    val motionEvent = it.args[0] as MotionEvent
                    val action = motionEvent.action
                    if (action == 2) Thread.currentThread().priority = 10
                    if (it.thisObject.objectHelper().getObjectOrNull("mWindowMode") == 2 && action == 2) {
                        blurClass.callStaticMethod("fastBlurDirectly", 1.0f, mLauncher.window)
                    }
                }

                if (isPad()) {
                    navStubViewClass.hookAfterAllMethods("onTouchEvent") {
                        val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                        blurClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 500L)
                    }
                    "com.miui.home.launcher.common.DeviceLevelUtils".hookBeforeMethod("isUseSimpleAnim") {
                        it.result = false
                    }
                }

              /*  navStubViewClass.hookBeforeMethod("appTouchResolution", MotionEvent::class.java) {
                    val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                    blurClass.callStaticMethod("fastBlurDirectly", 1.0f, mLauncher.window)
                }*/
            }

            else -> {
                "com.miui.home.launcher.common.DeviceLevelUtils".hookBeforeMethod("isUseSimpleAnim") {
                    it.result = false
                }

                mBlurClass.methodFinder().first {
                    name == "isUseCompleteBlurOnDev"
                }.createHook {
                    before {
                        when (blurLevel) {
                            1 -> returnConstant(true)
                        }
                    }
                }
            }
        }

    }
}
