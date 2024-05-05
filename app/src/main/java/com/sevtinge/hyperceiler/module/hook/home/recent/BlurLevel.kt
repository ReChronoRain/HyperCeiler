/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home.recent

import android.app.*
import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*

object BlurLevel : BaseHook() {
    private val blurLevel by lazy {
        mPrefsMap.getStringAsInt("home_recent_blur_level", 6)
    }

    override fun init() {
        val mBlurClass = loadClass("com.miui.home.launcher.common.BlurUtils")

        mBlurClass.methodFinder()
            .filterByName("getBlurType")
            .single().createHook {
                when (blurLevel) {
                    5 -> returnConstant(2)
                    0 -> returnConstant(2)
                    2 -> returnConstant(1)
                    3 -> returnConstant(0)
                    4 -> returnConstant(0)
                }
            }

        when (blurLevel) {
            4 -> {
                mBlurClass.methodFinder()
                    .filterByName("isUseCompleteBlurOnDev")
                    .single().createHook {
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
                    if (it.thisObject.objectHelper()
                            .getObjectOrNull("mWindowMode") == 2 && action == 2
                    ) {
                        blurClass.callStaticMethod("fastBlurDirectly", 1.0f, mLauncher.window)
                    }
                }

                "com.miui.home.launcher.common.DeviceLevelUtils".hookBeforeMethod("isUseSimpleAnim") {
                    it.result = false
                }

                if (isPad()) {
                    navStubViewClass.hookAfterAllMethods("onTouchEvent") {
                        val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                        blurClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 500L)
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

                mBlurClass.methodFinder()
                    .filterByName("isUseCompleteBlurOnDev")
                    .single().createHook {
                        if (blurLevel == 1) returnConstant(true)
                    }
            }
        }

    }
}
