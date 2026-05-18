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
package com.sevtinge.hyperceiler.libhook.rules.barrage


import android.app.Notification
import android.service.notification.StatusBarNotification
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs


object AnyBarrage : BaseHook() {
    override fun init() {
        loadClass("com.xiaomi.barrage.service.NotificationMonitorService")
            .findMethod {
                name("filterNotification")
            }.createHook {
                before { param ->
                    val statusBarNotification =
                        param.args[0] as StatusBarNotification
                    val packageName = statusBarNotification.packageName

                    param.thisObject.getObjectFieldAs<ArrayList<String>>("mBarragePackageList").apply {
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
