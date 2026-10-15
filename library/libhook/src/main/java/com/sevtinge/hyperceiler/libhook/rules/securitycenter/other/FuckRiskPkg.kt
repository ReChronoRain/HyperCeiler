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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.other

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import java.util.ArrayList
import java.lang.reflect.Method

object FuckRiskPkg : BaseHook() {

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        legacyNotificationMethods
        return true
    }

    private val legacyNotificationMethods by lazy<List<Method>> {
        optionalMemberList("FuckRiskPkg") {
            it.findMethod {
                matcher {
                    usingEqStrings("riskPkgList", "key_virus_pkg_list", "show_virus_notification")
                }
            }
        }
    }

    override fun init() {
        legacyNotificationMethods.createHooks {
            returnConstant(null)
        }
        // HyperOS 4 builds the "malicious app found" notification here.
        runCatching {
            loadClass("com.miui.antivirus.service.VirusScanJobService")
                .findMethod { name("x"); parameterTypes(ArrayList::class.java) }
                .createHook {
                    returnConstant(null)
                }
        }
    }
}
