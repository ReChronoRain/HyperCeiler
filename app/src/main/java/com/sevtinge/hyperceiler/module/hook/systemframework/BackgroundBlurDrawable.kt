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
package com.sevtinge.hyperceiler.module.hook.systemframework

import android.graphics.Canvas
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class BackgroundBlurDrawable : IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        val classLoader = startupParam.javaClass.classLoader
        val mBackgroundBlurDrawableClass = classLoader?.let {
            XposedHelpers.findClassIfExists(
                "com.android.internal.graphics.drawable.BackgroundBlurDrawable",
                it
            )
        } ?: return
        // 为 BackgroundBlurDrawable 应当增加一个判断
        // 此处应该可以为AOSP提交修复补丁
        XposedBridge.hookAllMethods(
            mBackgroundBlurDrawableClass,
            "draw",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val canvas = param.args[0] as Canvas
                    if (!canvas.isHardwareAccelerated) {
                        logI(
                            "BackgroundBlurDrawable",
                            "android",
                            "BackgroundBlurDrawable canvas is not HardwareAccelerated."
                        )
                        param.result = null
                    }
                }
            })
    }
}
