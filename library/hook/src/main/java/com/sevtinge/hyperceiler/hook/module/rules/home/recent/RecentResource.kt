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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.rules.home.recent

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew
import com.sevtinge.hyperceiler.hook.utils.ResourcesHookData
import com.sevtinge.hyperceiler.hook.utils.ResourcesHookMap
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod
import de.robv.android.xposed.XC_MethodHook
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext

object RecentResource : HomeBaseHookNew() {
    private val hookMap = ResourcesHookMap<String, ResourcesHookData>()
    private fun hook(param: XC_MethodHook.MethodHookParam) {
        try {
            val resName = appContext.resources.getResourceEntryName(param.args[0] as Int)
            val resType = appContext.resources.getResourceTypeName(param.args[0] as Int)
            if (hookMap.isKeyExist(resName)) if (hookMap[resName]?.type == resType) {
                param.result = hookMap[resName]?.afterValue
            }
        } catch (_: Throwable) {
        }
    }

    val sRoundedCorner by lazy {
        mPrefsMap.getInt("task_view_corners", 20)
    }

    @Version(isPad = false, min = 600000000)
    private fun isNewHomeHook() {
        // thank nakixii
        hookAllMethods("com.miui.home.recents.util.WindowCornerRadiusUtil", "setWindowRadius",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    runCatching {
                        val f = param.method.declaringClass.getDeclaredField("sTaskViewCornerRadius")
                        f.isAccessible = true
                        f.setInt(null, sRoundedCorner)
                    }
                }
            })

        publicHook()
    }

    @Version(isPad = true, min = 450000000)
    private fun isNewHomeHookPad() {
        // thank nakixii
        hookAllMethods("com.miui.home.recents.util.WindowCornerRadiusUtil", "setWindowRadius",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    runCatching {
                        val f = param.method.declaringClass.getDeclaredField("sTaskViewCornerRadius")
                        f.isAccessible = true
                        f.setInt(null, sRoundedCorner)
                    }
                }
            })

        publicHook()
    }

    override fun initBase() {
        findAndHookMethod("com.miui.home.recents.util.WindowCornerRadiusUtil", "getTaskViewCornerRadius", object : MethodHook(){
            override fun before(param: MethodHookParam?) {
                param?.result = sRoundedCorner
            }
        })

        publicHook()
    }

    private fun publicHook() {
        Application::class.java.hookBeforeMethod("attach", Context::class.java) { it ->
            EzXposed.initAppContext(it.args[0] as Context)

            Resources::class.java.hookBeforeMethod("getBoolean", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimension", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimensionPixelOffset", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimensionPixelSize", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getInteger", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getText", Int::class.javaPrimitiveType) { hook(it) }

            val value = sRoundedCorner.toFloat()
            val value1 = mPrefsMap.getInt("task_view_header_height", -1).toFloat()
            if (value != -1f && value != 20f) {
                hookMap["recents_task_view_rounded_corners_radius_min"] =
                    ResourcesHookData("dimen", dp2px(value))
                hookMap["recents_task_view_rounded_corners_radius_max"] =
                    ResourcesHookData("dimen", dp2px(value))
            }
            if (value1 != -1f && value != 40f) hookMap["recents_task_view_header_height"] =
                ResourcesHookData("dimen", dp2px(value1))
        }
    }

}
