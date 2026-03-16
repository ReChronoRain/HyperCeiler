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
package com.sevtinge.hyperceiler.libhook.appbase.systemui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper
import io.github.kyuubiran.ezxhelper.android.util.ViewUtil.findViewByIdName
import java.util.Collections.synchronizedSet
import java.util.concurrent.ConcurrentHashMap

/**
 * 状态栏 Hook 基类
 *
 */
abstract class StatusBarHook : BaseHook() {

    // ==================== View 查找 ====================

    /** 按资源 id 名查找子视图 */
    protected fun <T : View> ViewGroup.findById(idName: String): T? {
        @Suppress("UNCHECKED_CAST")
        return findViewByIdName(idName) as? T
    }

    /** 按 tag 查找子视图 */
    protected fun <T : View> ViewGroup.findByTag(tag: String): T? {
        @Suppress("UNCHECKED_CAST")
        return findViewWithTag(tag) as? T
    }

    // ==================== StateFlow 操作 ====================

    /** 替换对象的字段值 */
    protected fun Any.replaceField(fieldName: String, value: Any?) {
        setObjectField(this, fieldName, value)
    }

    /** 读取 StateFlow 当前值 */
    protected fun getFlowValue(flow: Any?): Any? {
        return StateFlowHelper.getStateFlowValue(flow)
    }

    /** 设置 StateFlow 值 */
    protected fun setFlowValue(flow: Any?, value: Any?) {
        StateFlowHelper.setStateFlowValue(flow, value)
    }

    /** 创建只读 StateFlow */
    protected fun <T> readonlyFlow(value: T): Any {
        return StateFlowHelper.newReadonlyStateFlow(value)
    }

    // ==================== View 缓存 ====================

    protected val viewCache = ConcurrentHashMap<Int, MutableSet<ViewGroup>>()

    protected fun cacheView(key: Int, view: ViewGroup) {
        val set = viewCache.getOrPut(key) { synchronizedSet(mutableSetOf()) }
        set.removeAll { !it.isAttachedToWindow }
        set.add(view)
    }

    protected fun clearCache() {
        viewCache.clear()
    }

    // ==================== ID 管理 ====================

    /** 为注入的自定义 View 生成并缓存稳定 ID */
    private val customViewIds = ConcurrentHashMap<String, Int>()

    protected fun getOrCreateViewId(name: String): Int {
        return customViewIds.getOrPut(name) { View.generateViewId() }
    }

    /** 通过 View ID 查找子视图 */
    protected fun <T : View> ViewGroup.findByViewId(id: Int): T? {
        @Suppress("UNCHECKED_CAST")
        return findViewById<View>(id) as? T
    }

    /** 解析 SystemUI 的资源 ID */
    protected fun resolveSystemUIId(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "id", "com.android.systemui")
    }
}
