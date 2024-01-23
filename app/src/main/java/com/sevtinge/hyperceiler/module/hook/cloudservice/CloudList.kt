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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.cloudservice

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

// <string name="cloud_list">应用数据云同步补全</string>
// <string name="cloud_list_summary">手机可以同步小米创作\n国内国际版小米浏览器可以同时同步</string>
// <string name="cloud_creation_summary">小米创作的首次同步必须从小米创作的设置中启用授权\n否则会提示网络不可用或系统忙请稍后再试</string>
object CloudList : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("support_google_csp_sync")
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader).createHook {
            returnConstant(null)
        }
    }
}
