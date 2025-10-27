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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.other

import android.view.View
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.util.WeakHashMap

object DisableBottomBar : BaseHook() {
    private val lastAlpha = WeakHashMap<View, Float>()

    override fun init() {
        if (isMoreAndroidVersion(36)) {
            // 隐藏小窗小白条，外加沉浸
            loadClass("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationInfo")
                .methodFinder().filterByName("getFreeformBottomCaptionHeight").first()
                .createHook {
                    returnConstant(0)
                }

            loadClass("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationBottom")
                .methodFinder().filterByName("getBottomCaptionHeight").first()
                .createHook {
                    after {
                        // 只要一碰小白条这个方法就会疯狂执行，猫猫问号？
                        val mode = it.thisObject.getObjectFieldOrNullAs<Int>("mWindowingMode") ?: return@after
                        val host = it.thisObject.getObjectFieldOrNull("mMiuiDecorationRootViewHost") ?: return@after
                        val view = host.getObjectFieldOrNullAs<View>("mMiuiDecorationBottomView") ?: return@after

                        val desiredAlpha = if (mode == 5) 0f else 1f

                        val prev = lastAlpha[view]
                        if (prev != null && prev == desiredAlpha) return@after
                        lastAlpha[view] = desiredAlpha

                        view.post {
                            view.alpha = desiredAlpha
                        }
                    }
                }

        } else {
            loadClass("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.MiuiBottomDecoration")
                .methodFinder().filterByName("createBottomCaption").first()
                .createHook {
                    returnConstant(null)
                }
        }
    }
}
