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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object BatterySaverEnableHapticFeedback : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        hapticFeedbackMethodList
        return true
    }

    private val hapticFeedbackMethodList by lazy<List<Method>> {
        optionalMemberList("BatterySaverLockHapticFeedback") {
            it.findMethod {
                matcher {
                    declaredClass {
                        usingEqStrings("sys.haptic.motor", "power_center_haptic_feed_back_mode", "haptic_feedback_enabled")
                    }
                    usingEqStrings("power_center_haptic_feed_back_mode", "haptic_feedback_enabled")
                    modifiers = Modifier.PUBLIC
                    returnType = "void"
                    paramCount = 1
                    paramTypes("android.content.Context")
                }
            }
        }
    }

    override fun init() {
        hapticFeedbackMethodList.forEach {
            it.createHook {
                returnConstant(null)
            }
        }
    }
}

