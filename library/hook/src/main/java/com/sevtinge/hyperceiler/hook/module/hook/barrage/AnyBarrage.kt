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
package com.sevtinge.hyperceiler.hook.module.hook.barrage


import android.app.Notification
import android.service.notification.StatusBarNotification
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook


object AnyBarrage : BaseHook() {
    override fun init() {
        loadClass("com.xiaomi.barrage.service.NotificationMonitorService").methodFinder()
            .filterByName("filterNotification")
            .first().createHook {
                before { param ->
                    val statusBarNotification =
                        param.args[0] as StatusBarNotification
                    val packageName = statusBarNotification.packageName

                    param.thisObject.getObjectFieldOrNullAs<ArrayList<String>>("mBarragePackageList")!!.apply {
                        if (!contains(packageName)) {
                            add(statusBarNotification.packageName)
                        }
                    }

                    if (statusBarNotification.shouldBeFiltered()) {
                        param.result = true
                    }
                }
            }
    }

    object NotificationCache {
        private const val MAX_SIZE = 100
        private val cache = LinkedHashSet<String>()
        fun check(string: String): Boolean {
            val result = cache.add(string)
            if (cache.size > MAX_SIZE) cache.remove(cache.first())
            return result
        }
    }

    private fun StatusBarNotification.shouldBeFiltered(): Boolean {
        val extras = notification.extras
        val key = "${extras.getCharSequence("android.title")}: ${extras.getCharSequence("android.text")}"
        val isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        return !isClearable || isGroupSummary || !NotificationCache.check(key)
    }

}
