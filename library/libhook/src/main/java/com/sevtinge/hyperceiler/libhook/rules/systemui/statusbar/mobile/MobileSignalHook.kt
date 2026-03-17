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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile

import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.appbase.systemui.StatusBarHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.miuiMobileIconBinder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileViewHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethodAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIntField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

/**
 * 移动信号 Hook 扩展基类
 *
 * 继承 [StatusBarHook]，封装 mobile 场景的通用 Hook 入口和工具。
 */
abstract class MobileSignalHook : StatusBarHook() {

    /**
     * Hook ModernStatusBarMobileView.constructAndBind
     * @param callback 回调 (rootView, subId)
     */
    protected fun hookConstructAndBind(callback: (ViewGroup, Int) -> Unit) {
        modernStatusBarMobileView.methodFinder()
            .filterByName("constructAndBind")
            .single()
            .createAfterHook { param ->
                val rootView = param.result as? ViewGroup ?: return@createAfterHook
                val subId = rootView.getIntField("subId")
                try {
                    callback(rootView, subId)
                } catch (e: Throwable) {
                    XposedLog.e(TAG, lpparam.packageName, "hookConstructAndBind callback error", e)
                }
            }
    }

    /**
     * 信号反色逻辑
     *
     * @param callback 回调 (rootView, darkInfo)
     */
    protected fun hookDarkMode(callback: (ViewGroup, DarkInfo) -> Unit) {
        miuiMobileIconBinder.methodFinder()
            .filterByName("bind")
            .single()
            .createAfterHook { param ->
                val container = param.args[0] as? ViewGroup ?: return@createAfterHook
                val binding = param.result ?: return@createAfterHook

                val tintFlow = findTintLightColorFlow(binding)
                if (tintFlow == null) {
                    XposedLog.w(TAG, lpparam.packageName, "hookDarkMode: tintLightColorFlow not found")
                    return@createAfterHook
                }

                MobileViewHelper.collectFlow(container, tintFlow) { triple ->
                    try {
                        callback(container, extractDarkInfo(triple))
                    } catch (e: Throwable) {
                        XposedLog.e(TAG, lpparam.packageName, "hookDarkMode flow error", e)
                    }
                }
            }
    }

    /** 从 binding 对象中找到 tintLightColorFlow（值为 Triple 的 StateFlowImpl 字段） */
    private fun findTintLightColorFlow(binding: Any): Any? {
        for (field in binding.javaClass.declaredFields) {
            try {
                field.isAccessible = true
                val value = field.get(binding) ?: continue
                if (value.javaClass.simpleName == "StateFlowImpl") {
                    val currentValue = value.callMethodAs<Any>("getValue")
                    if (currentValue.javaClass.simpleName == "Triple") {
                        return value
                    }
                }
            } catch (_: Throwable) { continue }
        }
        return null
    }

    /** 从 Triple(isUseTint, isLight, color) 提取 DarkInfo */
    private fun extractDarkInfo(triple: Any): DarkInfo {
        val cls = triple.javaClass
        return DarkInfo.fromTintLightColor(
            isUseTint = cls.getMethod("getFirst").invoke(triple) as Boolean,
            isLight = cls.getMethod("getSecond").invoke(triple) as Boolean,
            color = cls.getMethod("getThird").invoke(triple) as Int
        )
    }

    /**
     * Hook MiuiMobileIconBinder.bind，在官方 bind 完成后回调。
     * 可用于修改已有控件属性/布局，或注入新组件。
     */
    protected fun hookBind(callback: (ViewGroup, Any) -> Unit) {
        miuiMobileIconBinder.methodFinder()
            .filterByName("bind")
            .single()
            .createAfterHook { param ->
                val container = param.args[0] as? ViewGroup ?: return@createAfterHook
                val binding = param.result ?: return@createAfterHook
                try {
                    callback(container, binding)
                } catch (e: Throwable) {
                    XposedLog.e(TAG, lpparam.packageName, "hookBind callback error", e)
                }
            }
    }

    // ==================== 信号相关工具 ====================

    protected fun getDefaultDataSubId(): Int {
        return SubscriptionManager.getDefaultDataSubscriptionId()
    }

    protected fun forEachMobileView(subId: Int, callback: (View) -> Unit) {
        MobileViewHelper.forEachMobileView(subId, viewCache, callback)
    }
}
