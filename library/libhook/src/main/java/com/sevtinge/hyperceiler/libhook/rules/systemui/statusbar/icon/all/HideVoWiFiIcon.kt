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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all

import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setBooleanField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object HideVoWiFiIcon : BaseHook() {
    private val hideVoWifi by lazy {
        PrefsBridge.getBoolean("system_ui_status_bar_icon_vowifi")
    }
    private val hideVolte by lazy {
        PrefsBridge.getBoolean("system_ui_status_bar_icon_volte")
    }

    override fun init() {
        val operatorConfig = loadClass($$"com.miui.interfaces.IOperatorCustomizedPolicy$OperatorConfig")

        // OS 2.x / OS 3.0: OperatorConfig still has an <init>, so keep the original
        // constructor hook.
        val constructors = operatorConfig.declaredConstructors
        if (constructors.isNotEmpty()) {
            constructors[0].createHook {
                after { applyOperatorConfig(it.thisObject) }
            }
            return
        }

        // HyperOS 3.3 (Android 17): R8 inlined OperatorConfig's constructor into
        // MiuiOperatorCustomizedPolicy#getMiuiOperatorConfig -- the dex only contains
        // new-instance plus iput, with no <init> left at all, so constructors[0]
        // throws ArrayIndexOutOfBoundsException(length=0). Hook the method that now
        // holds the construction instead and overwrite the fields on its result,
        // which has the same effect as the old constructor hook.
        loadClass("com.android.systemui.MiuiOperatorCustomizedPolicy")
            .findMethod { name("getMiuiOperatorConfig") }
            .createAfterHook { param ->
                applyOperatorConfig(param.result ?: return@createAfterHook)
            }
    }

    private fun applyOperatorConfig(config: Any) {
        config.setBooleanField("hideVowifi", hideVoWifi)
        config.setBooleanField("hideVolte", hideVolte)
    }
}
