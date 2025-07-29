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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import de.robv.android.xposed.*


object NotificationImportanceHyperOSFix : BaseHook() {
    override fun init() {
        if (isMoreAndroidVersion(35)) {
            loadClass("com.android.systemui.statusbar.notification.collection.coordinator.StackCoordinator\$attach\$1")
                .methodFinder().filterByName("onAfterRenderList")
                .first().createBeforeHook { param ->
                    val mNotificationEntries = param.args[0] as List<*>
                    if (mNotificationEntries.isNotEmpty()) {
                        val list = ArrayList<Any>()
                        mNotificationEntries.forEach {
                            val representativeEntry = invokeMethodBestMatch(it!!, "getRepresentativeEntry")!!
                            val ranking =
                                XposedHelpers.getObjectField(representativeEntry, "mRanking")
                            val importance =
                                XposedHelpers.callMethod(ranking, "getImportance") as Int
                            if (importance > 1) list.add(it)
                        }
                        if (list.size != mNotificationEntries.size) param.args[0] = list
                    }
                }
        } else {
            loadClass("com.android.systemui.statusbar.phone.NotificationIconAreaController")
                .methodFinder().filterByName("updateStatusBarIcons")
                .first().createBeforeHook { param ->
                    val mNotificationEntries =
                        getObjectOrNullAs<List<Any>>(param.thisObject, "mNotificationEntries")!!
                    if (mNotificationEntries.isNotEmpty()) {
                        val list = ArrayList<Any>()
                        mNotificationEntries.forEach {
                            val representativeEntry =
                                invokeMethodBestMatch(it, "getRepresentativeEntry")!!
                            val importance =
                                invokeMethodBestMatch(representativeEntry, "getImportance") as Int

                            if (importance > 1) list.add(it)
                        }
                        if (list.size != mNotificationEntries.size) {
                            setObject(param.thisObject, "mNotificationEntries", list)
                        }
                    }
                }
        }
    }
}
