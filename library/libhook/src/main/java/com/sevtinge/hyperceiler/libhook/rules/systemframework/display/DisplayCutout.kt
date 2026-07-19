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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.display

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getStaticObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField

object DisplayCutout : BaseHook() {
    override fun init() {
        val displayCutoutClass = findClass("android.view.DisplayCutout")
        val noCutout = displayCutoutClass.getStaticObjectField("NO_CUTOUT")

        displayCutoutClass.findAllMethods { name("pathAndDisplayCutoutFromSpec") }
            .createBeforeHooks {
                it.args[0] = "M 0,0 H 0 V 0 Z"
                // Android 16 prioritizes rectSpec when it is present.
                // An empty string causes the framework to return NULL_PAIR,
                // which propagates a null DisplayCutout to secondary displays.
                // Use null so the synthetic empty pathSpec remains effective.
                it.args[1] = null
            }

        listOf("fromResourcesRectApproximation", "fromSpec").forEach { methodName ->
            displayCutoutClass.findAllMethods { name(methodName) }
                .createAfterHooks {
                    if (it.result == null) {
                        it.result = noCutout
                    }
                }
        }

        findClass("com.android.server.display.LogicalDisplay").findMethod {
            name("getDisplayInfoLocked")
        }.createAfterHook {
            ensureNonNullDisplayCutout(it.result, noCutout)
        }
    }

    private fun ensureNonNullDisplayCutout(displayInfo: Any?, noCutout: Any?) {
        if (displayInfo == null || noCutout == null) return
        if (displayInfo.getObjectField("displayCutout") == null) {
            displayInfo.setObjectField("displayCutout", noCutout)
        }
    }
}
