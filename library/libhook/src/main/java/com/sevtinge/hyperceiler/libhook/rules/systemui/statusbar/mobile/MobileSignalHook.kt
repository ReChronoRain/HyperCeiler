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
import android.view.ViewTreeObserver
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.appbase.systemui.StatusBarHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.miuiMobileIconBinder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileViewHelper
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getIntField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

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
        modernStatusBarMobileView.findMethod { name("constructAndBind") }
            .createAfterHook { param ->
                handleConstructAndBind(param.result, callback)
            }
    }

    private fun handleConstructAndBind(
        result: Any?,
        callback: (ViewGroup, Int) -> Unit
    ) {
        val rootView = result as? ViewGroup ?: return
        val subId = rootView.getIntField("subId")
        try {
            callback(rootView, subId)
        } catch (e: Throwable) {
            XposedLog.e(TAG, lpparam.packageName, "hookConstructAndBind callback error", e)
        }
    }

    /**
     * 信号反色逻辑
     *
     * @param callback 回调 (rootView, darkInfo)
     */
    protected fun hookDarkMode(callback: (ViewGroup, DarkInfo) -> Unit) {
        val observers = WeakHashMap<ViewGroup, TintObserver>()
        registerHotReloadCleanup {
            observers.values.toList().forEach { it.dispose() }
            observers.clear()
        }

        fun observe(container: ViewGroup, binding: Any) {
            try {
                val source = MobileTintSource.find(binding) ?: return
                val observer = observers[container]
                if (observer != null) {
                    observer.source = source
                    observer.dispatch(force = true)
                } else {
                    TintObserver(container, source, callback).also {
                        observers[container] = it
                        it.start()
                    }
                }
            } catch (e: Throwable) {
                XposedLog.e(TAG, lpparam.packageName, "hookDarkMode source error", e)
            }
        }

        miuiMobileIconBinder.findMethod { name("bind") }
            .createAfterHook { param ->
                val container = param.args[0] as? ViewGroup ?: return@createAfterHook
                val binding = param.result ?: return@createAfterHook
                observe(container, binding)
            }

        // ROM 可能内联 bind；构造结束时再从视图取得实际 binding，避免漏装观察器。
        modernStatusBarMobileView.findMethod { name("constructAndBind") }
            .createAfterHook { param ->
                val container = param.result as? ViewGroup ?: return@createAfterHook
                val binding = runCatching { container.getObjectField("binding") }.getOrNull()
                    ?: return@createAfterHook
                observe(container, binding)
            }
    }

    /**
     * SystemUI 的 onIconTintChanged 和 onDarkChanged 都会写同一条 Flow。
     * 绘制前读取它的最新值，覆盖应用背景变化；颜色不变时不重复刷新图标。
     * 监听随视图 attach/detach 注册和移除，热重载时也显式清理。
     */
    private inner class TintObserver(
        container: ViewGroup,
        var source: MobileTintSource,
        private val callback: (ViewGroup, DarkInfo) -> Unit
    ) : View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener {
        private val containerRef = WeakReference(container)
        private var tree: ViewTreeObserver? = null
        private var lastInfo: DarkInfo? = null
        private var errorReported = false

        fun start() {
            val container = containerRef.get() ?: return
            container.addOnAttachStateChangeListener(this)
            dispatch(force = true)
            if (container.isAttachedToWindow) onViewAttachedToWindow(container)
        }

        fun dispatch(force: Boolean = false) {
            val container = containerRef.get() ?: return
            try {
                val info = source.read()
                if (force || info != lastInfo) {
                    callback(container, info)
                    lastInfo = info
                }
                errorReported = false
            } catch (e: Throwable) {
                if (!errorReported) {
                    XposedLog.e(TAG, lpparam.packageName, "hookDarkMode callback error", e)
                    errorReported = true
                }
            }
        }

        override fun onPreDraw(): Boolean {
            dispatch()
            return true
        }

        override fun onViewAttachedToWindow(view: View) {
            removePreDrawListener()
            tree = view.viewTreeObserver.also { it.addOnPreDrawListener(this) }
            // bind 时双排容器可能尚未创建；attach 后必须应用一次当前颜色。
            dispatch(force = true)
        }

        override fun onViewDetachedFromWindow(view: View) {
            removePreDrawListener()
        }

        private fun removePreDrawListener() {
            tree?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
            tree = null
        }

        fun dispose() {
            removePreDrawListener()
            containerRef.get()?.removeOnAttachStateChangeListener(this)
        }
    }

    /**
     * Hook MiuiMobileIconBinder.bind，在官方 bind 完成后回调。
     * 可用于修改已有控件属性/布局，或注入新组件。
     */
    protected fun hookBind(callback: (ViewGroup, Any) -> Unit) {
        miuiMobileIconBinder.findMethod { name("bind") }
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
