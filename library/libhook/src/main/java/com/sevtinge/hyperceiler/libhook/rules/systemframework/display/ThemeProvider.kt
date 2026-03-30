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
import com.sevtinge.hyperceiler.libhook.utils.guard.RearScreenFlowGuard
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import io.github.libxposed.api.XposedInterface
import miui.drm.DrmManager
import miui.drm.ThemeReceiver
import java.util.ArrayDeque

class ThemeProvider : BaseHook() {
    // validateTheme may nest on one thread, so temporary hooks need to be unwound by call depth.
    private val hookStack = ThreadLocal.withInitial<ArrayDeque<List<XposedInterface.HookHandle>>> { ArrayDeque() }

    override fun init() {
        try {
            ThemeReceiver::class.java.methodFinder().filterByName("validateTheme").first().createHook {
                before {
                    val systemContext = ContextUtils.getContextNoError(ContextUtils.FlAG_ONLY_ANDROID)
                    val hooks = if (RearScreenFlowGuard.isRearScreenActivityActive(systemContext)) {
                        emptyList()
                    } else {
                        runCatching {
                            DrmManager::class.java.methodFinder().filterByName("isLegal").toList().createHooks {
                                returnConstant(DrmManager.DrmResult.DRM_SUCCESS)
                            }
                        }.onFailure { throwable ->
                            XposedLog.e(TAG, packageName, throwable)
                        }.getOrDefault(emptyList())
                    }

                    currentHookStack().addLast(hooks)
                }
                after {
                    val stack = currentHookStack()
                    if (!stack.isEmpty()) {
                        stack.removeLast().forEach { it.unhook() }
                    }
                    if (stack.isEmpty()) {
                        hookStack.remove()
                    }
                }
            }
        } catch (t: Throwable) {
            XposedLog.e(TAG, packageName, t)
        }
    }

    private fun currentHookStack(): ArrayDeque<List<XposedInterface.HookHandle>> =
        checkNotNull(hookStack.get())
}
