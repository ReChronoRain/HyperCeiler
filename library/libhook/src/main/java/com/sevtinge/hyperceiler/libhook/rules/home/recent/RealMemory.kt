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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home.recent

import android.app.ActivityManager
import android.content.Context
import android.text.format.Formatter
import android.widget.TextView
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.text.DecimalFormat

object RealMemory : HomeBaseHookNew() {

    @Version(isPad = true)
    fun initPadLoad() {
        val clazz = findClass("com.miui.home.recents.views.RecentsDecorations")
        initHook(clazz)
    }


    override fun initBase() {
        val clazz = findClass("com.miui.home.recents.views.RecentsContainer")
        initHook(clazz)
    }

    private fun initHook(clazz: Class<*>) {
        lateinit var context: Context
        var memoryInfo1StringId: Int? = null
        var memoryInfo2StringId: Int? = null

        fun Any.formatSize(): String = Formatter.formatFileSize(context, this as Long)

        clazz.apply {
            declaredConstructors.constructorFinder()
                .filterByParamCount(2)
                .firstOrNull()?.createHook {
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
                } ?: XposedLog.e(TAG, lpparam.packageName, "Constructor not found")

            methodFinder()
                .filterByName("refreshMemoryInfo")
                .firstOrNull()?.createHook {
                    before {
                        it.result = null
                        val memoryInfo = ActivityManager.MemoryInfo()
                        val activityManager =
                            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        activityManager.getMemoryInfo(memoryInfo)
                        var totalMem =
                            "\\d+\\.\\d+".toRegex().find(memoryInfo.totalMem.formatSize())?.value
                        val extmSize = getProp("persist.miui.extm.bdsize")
                        var extmMem = ""
                        if (!getProp("persist.miui.extm.enable").equals("0")) {
                            try {
                                val number = extmSize.toDouble() / 1024
                                val df = DecimalFormat("0.00")
                                extmMem = "+" + df.format(number).toString()
                            } catch (e: NumberFormatException) {
                                XposedLog.e(
                                    TAG, lpparam.packageName, "Get extm size failed by: $e"
                                )
                            }
                        }
                        totalMem = "$totalMem$extmMem GB"
                        val availMem = memoryInfo.availMem.formatSize()
                        (it.thisObject.getObjectField("mTxtMemoryInfo1") as TextView).text =
                            context.getString(memoryInfo1StringId!!, availMem, totalMem)
                        (it.thisObject.getObjectField("mTxtMemoryInfo2") as TextView).text =
                            context.getString(memoryInfo2StringId!!, availMem, totalMem)
                    }
                } ?: XposedLog.e(TAG, lpparam.packageName, "refreshMemoryInfo method not found")
        }
    }
}
