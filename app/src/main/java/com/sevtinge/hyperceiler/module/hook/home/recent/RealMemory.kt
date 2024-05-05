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

import android.annotation.*
import android.app.*
import android.content.*
import android.text.format.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*

object RealMemory : BaseHook() {
    @SuppressLint("DiscouragedApi")
    override fun init() {
        lateinit var context: Context
        var memoryInfo1StringId: Int? = null
        var memoryInfo2StringId: Int? = null

        fun Any.formatSize(): String = Formatter.formatFileSize(context, this as Long)

        val recentContainerClass = loadClass(
            when (IS_TABLET) {
                false -> "com.miui.home.recents.views.RecentsContainer"
                true -> "com.miui.home.recents.views.RecentsDecorations"
            }
        )

        recentContainerClass.declaredConstructors.constructorFinder()
            .filterByParamCount(2)
            .first().createHook {
                after {
                    context = it.args[0] as Context
                    memoryInfo1StringId = context.resources.getIdentifier(
                        "status_bar_recent_memory_info1",
                        "string",
                        "com.miui.home"
                    )
                    memoryInfo2StringId = context.resources.getIdentifier(
                        "status_bar_recent_memory_info2",
                        "string",
                        "com.miui.home"
                    )
                }
            }

        recentContainerClass.methodFinder()
            .filterByName("refreshMemoryInfo")
            .first().createHook {
                before {
                    it.result = null
                    val memoryInfo = ActivityManager.MemoryInfo()
                    val activityManager =
                        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    activityManager.getMemoryInfo(memoryInfo)
                    val totalMem = memoryInfo.totalMem.formatSize()
                    val availMem = memoryInfo.availMem.formatSize()
                    (it.thisObject.getObjectField("mTxtMemoryInfo1") as TextView).text =
                        context.getString(memoryInfo1StringId!!, availMem, totalMem)
                    (it.thisObject.getObjectField("mTxtMemoryInfo2") as TextView).text =
                        context.getString(memoryInfo2StringId!!, availMem, totalMem)
                }
            }
    }
}
