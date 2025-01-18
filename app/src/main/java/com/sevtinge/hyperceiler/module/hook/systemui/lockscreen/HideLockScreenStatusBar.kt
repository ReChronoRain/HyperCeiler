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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*

object HideLockScreenStatusBar : BaseHook() {
    override fun init() {
        if (isMoreAndroidVersion(35)) {
            loadClassOrNull("com.android.systemui.statusbar.phone.CentralSurfacesImpl")!!.methodFinder()
                .filterByName("updateIsKeyguard")
                .single().createHook {
                    after { param ->
                        val shadeControllerImpl =
                            XposedHelpers.getObjectField(param.thisObject, "mShadeController")
                        val getNpvc =
                            XposedHelpers.getObjectField(shadeControllerImpl, "mNpvc")
                        val mKeyguardStatusBar = XposedHelpers.getObjectField(
                            XposedHelpers.callMethod(getNpvc, "get"),
                            "mKeyguardStatusBarViewController"
                        )

                        XposedHelpers.setObjectField(mKeyguardStatusBar, "mKeyguardStatusBarAnimateAlpha", 0.0f)
                    }
                }
        } else {
            hookAllMethods(
                "com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader,
                "updateIsKeyguard",
                object : MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val shadeControllerImpl =
                            XposedHelpers.getObjectField(param.thisObject, "mShadeController")

                        val mKeyguardStatusBar = XposedHelpers.getObjectField(
                            XposedHelpers.getObjectField(
                                shadeControllerImpl,
                                "mNotificationPanelViewController"
                            ), "mKeyguardStatusBar"
                        ) as View
                        mKeyguardStatusBar.translationY = -999f
                    }
                }
            )
        }
    }
}
