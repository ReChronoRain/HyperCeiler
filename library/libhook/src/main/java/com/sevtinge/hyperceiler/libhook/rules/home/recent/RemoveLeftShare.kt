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

import android.view.ViewGroup
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object RemoveLeftShare : BaseHook() {
    override fun init() {
        if (isPad()) {
            loadClass("com.miui.home.recents.views.RecentsTopWindowDropTargetWorldCirculate")
        } else {
            loadClass("com.miui.home.recents.views.RecentsWorldCirculateAndSmallWindowCrop")
        }.methodFinder().filterByName("initViewDisplayInDrag")
            .first().createHook {
                before {
                    it.thisObject.setObjectField("mIsSupportWorldcirculate", false)
                }
                after {
                    val mWorldcirculateContent =
                        it.thisObject.getObjectField("mWorldcirculateContent") as ViewGroup
                    mWorldcirculateContent.visibility = ViewGroup.GONE
                }
            }
    }
}
