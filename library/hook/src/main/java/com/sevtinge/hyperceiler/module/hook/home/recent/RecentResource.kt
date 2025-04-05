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
package com.sevtinge.hyperceiler.module.hook.home.recent

import android.app.*
import android.content.*
import android.content.res.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.*

object RecentResource : BaseHook() {
    private val hookMap = ResourcesHookMap<String, ResourcesHookData>()
    private fun hook(param: XC_MethodHook.MethodHookParam) {
        try {
            val resName = appContext.resources.getResourceEntryName(param.args[0] as Int)
            val resType = appContext.resources.getResourceTypeName(param.args[0] as Int)
            if (hookMap.isKeyExist(resName)) if (hookMap[resName]?.type == resType) {
                param.result = hookMap[resName]?.afterValue
            }
        } catch (ignore: Exception) {
        }
    }

    val sRoundedCorner by lazy {
        mPrefsMap.getInt("task_view_corners", 20)
    }

    override fun init() {

        findAndHookMethod("com.miui.home.recents.util.WindowCornerRadiusUtil", "getTaskViewCornerRadius", object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook(){
            override fun before(param: MethodHookParam?) {
                param?.result = sRoundedCorner
            }
        })
        Application::class.java.hookBeforeMethod("attach", Context::class.java) { it ->
            EzXHelper.initHandleLoadPackage(lpparam)
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)
            EzXHelper.initAppContext(it.args[0] as Context)

            Resources::class.java.hookBeforeMethod("getBoolean", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimension", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimensionPixelOffset", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimensionPixelSize", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getInteger", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getText", Int::class.javaPrimitiveType) { hook(it) }

            val value = sRoundedCorner.toFloat()
            val value1 = mPrefsMap.getInt("task_view_header_height", -1).toFloat()
            if (value != -1f && value != 20f) {
                hookMap["recents_task_view_rounded_corners_radius_min"] = ResourcesHookData("dimen", dp2px(value))
                hookMap["recents_task_view_rounded_corners_radius_max"] = ResourcesHookData("dimen", dp2px(value))
            }
            if (value1 != -1f && value != 40f) hookMap["recents_task_view_header_height"] =
                ResourcesHookData("dimen", dp2px(value1))
        }
    }

}
