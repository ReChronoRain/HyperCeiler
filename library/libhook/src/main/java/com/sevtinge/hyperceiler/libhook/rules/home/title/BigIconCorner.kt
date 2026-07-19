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
package com.sevtinge.hyperceiler.libhook.rules.home.title

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField

object BigIconCorner : BaseHook() {
    private val maMlHostViewClass by lazy {
        loadClass("com.miui.home.launcher.maml.MaMlHostView")
    }

    override fun init() {
        loadClass("com.miui.home.launcher.bigicon.BigIconUtil").findAllMethods { filter { name == "getCroppedFromCorner" && parameterCount == 4 } }.createHooks {
            before {
                it.args[0] = 2
                it.args[1] = 2
            }
        }

        maMlHostViewClass.findMethod { name("getCornerRadius") }.createHook {
                before {
                    it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
                }
            }

        maMlHostViewClass.findMethod { name("computeRoundedCornerRadius"); paramCount(1) }.createHook {
                before {
                    it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
                }
            }

        loadClass("com.miui.home.launcher.LauncherAppWidgetHostView").findMethod { name("computeRoundedCornerRadius"); paramCount(1) }.createHook {
                before {
                    it.result = it.thisObject.getObjectField("mEnforcedCornerRadius") as Float
                }
            }
    }
}
