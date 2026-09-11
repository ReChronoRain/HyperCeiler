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
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen

import android.view.View
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.newReadonlyStateFlow
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

object HideLockScreenStatusBar : BaseHook() {
    override fun init() {
        val controller = loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardStatusBarViewController")
        val fields = generateSequence(controller) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }.map { it.name }.toSet()
        val hasViewModel = "mKeyguardStatusBarViewModel" in fields
        if (!hasViewModel && "mKeyguardStatusBarAnimateAlpha" !in fields) {
            // HyperOS 3 on Android 17 removed both controller fields. Its dedicated
            // keyguard view still owns visibility, independently of the unlocked bar.
            loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardStatusBarView")!!
                .findMethod { name("setVisibility"); parameterTypes(Int::class.java) }
                .createBeforeHook { it.args[0] = View.INVISIBLE }
            loadClassOrNull("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView")!!
                .findMethod { name("onAttachedToWindow") }
                .createAfterHook { (it.thisObject as View).visibility = View.INVISIBLE }
            return
        }
        loadClassOrNull("com.android.systemui.statusbar.phone.CentralSurfacesImpl")!!.findMethod { name("updateIsKeyguard") }.createHook {
                after { param ->
                    val shadeControllerImpl =
                        param.thisObject.getObjectField("mShadeController")

                    val mKeyguardStatusBar =
                        shadeControllerImpl!!.getObjectField("mNpvc")!!
                            .callMethod("get")!!
                            .getObjectField("mKeyguardStatusBarViewController")

                    if (hasViewModel) {
                        mKeyguardStatusBar!!.getObjectField("mKeyguardStatusBarViewModel")!!
                            .setObjectField("isVisible", newReadonlyStateFlow(false))
                    } else {
                        mKeyguardStatusBar!!.setObjectField("mKeyguardStatusBarAnimateAlpha", 0.0f)
                    }
                }

            }

    }
}
