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
package com.sevtinge.hyperceiler.module.hook.systemframework.display

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import de.robv.android.xposed.XC_MethodHook
import miui.drm.DrmManager
import miui.drm.ThemeReceiver

class ThemeProvider : BaseHook() {
    override fun init() {
        var hook: List<XC_MethodHook.Unhook>? = null
        try {
            ThemeReceiver::class.java.methodFinder().filterByName("validateTheme").first().createHook {
                before {
                    hook = DrmManager::class.java.methodFinder().filterByName("isLegal").toList().createHooks {
                        returnConstant(DrmManager.DrmResult.DRM_SUCCESS)
                    }
                }
                after {
                    hook?.forEach { it.unhook() }
                }
            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}
