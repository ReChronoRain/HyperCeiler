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

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.guard.RearScreenFlowGuard
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.chainMethod
import io.github.libxposed.api.XposedInterface
import miui.drm.DrmManager
import miui.drm.ThemeReceiver

class ThemeProvider : BaseHook() {
    companion object {
        private val sBypassDrmCheck = ThreadLocal<Boolean>()
    }

    override fun init() {
        try {
            chainAllMethods(DrmManager::class.java, "isLegal",
                XposedInterface.Hooker { chain ->
                    if (sBypassDrmCheck.get() == true) {
                        return@Hooker DrmManager.DrmResult.DRM_SUCCESS
                    }
                    chain.proceed()
                }
            )

            ThemeReceiver::class.java.chainMethod("validateTheme") {
                val systemContext = ContextUtils.getContextNoError(ContextUtils.FlAG_ONLY_ANDROID)
                if (RearScreenFlowGuard.isRearScreenActivityActive(systemContext)) {
                    return@chainMethod proceed()
                } else {
                    sBypassDrmCheck.set(true)
                }
                try {
                    proceed()
                } finally {
                    sBypassDrmCheck.remove()
                }
            }
        } catch (t: Throwable) {
            XposedLog.e(TAG, packageName, t)
        }
    }
}
