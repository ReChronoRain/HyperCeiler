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
package com.sevtinge.hyperceiler.libhook.rules.mtb

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

object BypassAuthentication : BaseHook() {
    override fun init() {
        if (isMoreHyperOSVersion(3f)) {
            runCatching {
                loadClass("com.xiaomi.mtb.MtbAppBase").findMethod { name("getMiServerPermissionClass") }.createAfterHook {
                        it.result = 0L
                    }
            }
        } else {
            runCatching {
                loadClass("com.xiaomi.mtb.MtbApp").findMethod { name("getMiServerPermissionClass") }.createAfterHook {
                        it.result = 0L
                    }
            }
        }
    }
}
