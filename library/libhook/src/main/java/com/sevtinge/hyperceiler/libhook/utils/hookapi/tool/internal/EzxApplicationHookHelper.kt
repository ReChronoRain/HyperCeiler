/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.internal

import android.app.Application
import android.content.Context
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.ContextConsumer
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.IApplicationHook
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam

/**
 * Application 生命周期 Hook 工具
 * 封装 Application.attach 的 Hook 和回调管理
 */
internal object EzxApplicationHookHelper {

    private const val TAG = "EzxApplicationHookHelper"

    private val applicationHooks = mutableListOf<IApplicationHook>()
    private var isApplicationHooked = false
    private val applicationHookLock = Any()

    /**
     * 注册 Application 生命周期回调
     *
     * @param hook 回调实例
     */
    fun registerApplicationHook(hook: IApplicationHook) {
        synchronized(applicationHookLock) {
            applicationHooks.add(hook)
            ensureApplicationHooked()
        }
    }

    /**
     * 注册 Application 生命周期回调
     *
     * @param before attach 之前的回调，可为 null
     * @param after attach 之后的回调，可为 null
     */
    fun registerApplicationHook(
        before: ContextConsumer?,
        after: ContextConsumer?
    ) {
        registerApplicationHook(object : IApplicationHook {
            override fun onApplicationAttachBefore(context: Context) {
                before?.accept(context)
            }

            override fun onApplicationAttachAfter(context: Context) {
                after?.accept(context)
            }
        })
    }

    /**
     * 仅注册 Application attach 之后的回调
     *
     * @param callback 回调函数
     */
    fun runOnApplicationAttach(callback: ContextConsumer) {
        registerApplicationHook(null, callback)
    }

    /**
     * 确保 Application.attach 已被 Hook
     */
    private fun ensureApplicationHooked() {
        if (isApplicationHooked) return

        synchronized(applicationHookLock) {
            if (isApplicationHooked) return

            try {
                EzxHookHelper.findAndHookMethod(
                    Application::class.java,
                    "attach",
                    Context::class.java,
                    object : IMethodHook {
                        override fun before(param: HookParam) {
                            val context = param.args[0] as? Context ?: return
                            applicationHooks.forEach { hook ->
                                try {
                                    hook.onApplicationAttachBefore(context)
                                } catch (_: Throwable) {
                                    // Keep fan-out resilient; hooks decide whether to report their own errors.
                                }
                            }
                        }

                        override fun after(param: HookParam) {
                            val context = param.args[0] as? Context ?: return
                            applicationHooks.forEach { hook ->
                                try {
                                    hook.onApplicationAttachAfter(context)
                                } catch (_: Throwable) {
                                    // Keep fan-out resilient; hooks decide whether to report their own errors.
                                }
                            }
                            XposedLog.d(TAG, "Application created! package: ${context.packageName}")
                        }
                    }
                )
                isApplicationHooked = true
            } catch (t: Throwable) {
                XposedLog.e(TAG, "Failed to hook Application.attach", t)
            }
        }
    }

    /**
     * 取消注册 Application 生命周期回调
     *
     * @param hook 要取消的回调实例
     */
    fun unregisterApplicationHook(hook: IApplicationHook) {
        synchronized(applicationHookLock) {
            applicationHooks.remove(hook)
        }
    }

    /**
     * 清除所有 Application 生命周期回调
     */
    fun clearApplicationHooks() {
        synchronized(applicationHookLock) {
            applicationHooks.clear()
        }
    }
}
