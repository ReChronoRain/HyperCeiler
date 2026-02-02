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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.mipad


import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.deoptimizeMethod
import io.github.kyuubiran.ezxhelper.core.extension.MemberExtension.isNotAbstract
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object RestoreEsc : BaseHook() {
    override fun init() {
        findClass("com.android.server.input.InputManagerServiceStubImpl").methodFinder()
            .filterByName("switchPadMode").single().deoptimizeMethod()
        findClass("com.android.server.input.InputManagerServiceStubImpl").methodFinder()
            .filterByName("init").filter { this.isNotAbstract }.single().deoptimizeMethod()
        findClass("com.android.server.input.config.InputCommonConfig").methodFinder()
            .filterByName("setPadMode").single().createBeforeHook {
                it.args[0] = false
            }
    }
}
