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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen

import android.view.View
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object HideLockScreenStatusBar : BaseHook() {
    override fun init() {
        loadClassOrNull("com.android.systemui.statusbar.phone.CentralSurfacesImpl")!!.methodFinder()
            .filterByName("updateIsKeyguard")
            .single().createHook {
                after { param ->
                    val shadeControllerImpl =
                        param.thisObject.getObjectField("mShadeController")

                    if (isMoreAndroidVersion(35)) {
                        val mKeyguardStatusBar =
                            shadeControllerImpl!!.getObjectField("mNpvc")!!
                                .callMethod("get")!!
                                .getObjectField("mKeyguardStatusBarViewController")

                        mKeyguardStatusBar.setObjectField("mKeyguardStatusBarAnimateAlpha", 0.0f)
                    } else {
                        val mKeyguardStatusBar = shadeControllerImpl!!
                            .getObjectField("mNotificationPanelViewController")!!
                            .getObjectField("mKeyguardStatusBar") as View

                        mKeyguardStatusBar.translationY = -999f
                    }
                }
            }

    }
}
