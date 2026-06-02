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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findViewByIdName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import io.github.lingqiqi5211.ezhooktool.core.findAllFields
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setBooleanField
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.WeakHashMap

class SidebarCommonAppLimit : BaseHook() {
    override fun useDexKit() = true

    private val limit by lazy {
        if (PrefsBridge.getBoolean("security_center_sidebar_common_app_limit_enable")) {
            PrefsBridge.getInt("security_center_sidebar_common_app_limit", DEFAULT_LIMIT).coerceAtLeast(1)
        } else {
            DEFAULT_LIMIT
        }
    }

    private val hideSuggest by lazy {
        PrefsBridge.getBoolean("disable_security_center_sidebar_show_suggest")
    }

    private var selectedCount = 0
    private var editActivityClass: Class<*>? = null
    private var editAllAdapterClass: Class<*>? = null
    private var editSelectedAdapterClass: Class<*>? = null
    private var editItemClass: Class<*>? = null
    private val wrappedSourceLists = WeakHashMap<Any, List<Pair<Field, Any?>>>()
    private val wrappedEditLists = WeakHashMap<Any, List<Pair<Field, Any?>>>()

    private val editActivityClassFromDex by lazy {
        optionalMember("DockEditActivityClassByQuickModels") {
            it.findMethod {
                matcher {
                    usingEqStrings(
                        "quickModels: ",
                        "tempList: "
                    )
                    returnType = "void"
                    paramTypes = listOf("java.util.List")
                }
            }.singleOrNull()?.declaredClass
        } as? Class<*>
    }

    private val addMethod by lazy {
        val editClass = editActivityClassFromDex ?: return@lazy null
        optionalMember("DockEditAddCommonAppV2") {
            it.getClassData(editClass)?.findMethod {
                matcher {
                    declaredClass = editClass.name
                    returnType = "void"
                    paramCount = 3
                    paramTypes(null, "int", null)
                    usingNumbers(DEFAULT_LIMIT)
                    addInvoke("Ljava/util/List;->size()I")
                    addInvoke {
                        name = "notifyItemRangeChanged"
                        paramCount = 3
                    }
                }
                findFirst = true
            }?.singleOrNull()
        } as? Method
    }

    private val removeMethod by lazy {
        val editClass = editActivityClassFromDex ?: return@lazy null
        val itemClass = addMethod?.parameterTypes?.getOrNull(2) ?: return@lazy null
        editClass.findMethodOrNull {
            voidReturnType()
            params(Integer.TYPE, itemClass)
        }
    }

    private val sourceCollectMethod by lazy {
        optionalMember("DockSidebarCollectApps") {
            it.findMethod {
                matcher {
                    paramCount = 6
                    usingNumbers(DEFAULT_SOURCE_LIMIT)
                    addInvoke("Ljava/util/List;->size()I")
                }
            }.singleOrNull()
        } as? Method
    }

    private val sourceListFields by lazy<List<Field>> {
        val method = sourceCollectMethod ?: return@lazy emptyList()
        optionalMemberList("DockSidebarCollectListFields") {
            it.findField {
                matcher {
                    declaredClass = method.declaringClass.name
                    type = "java.util.List"
                    addReadMethod {
                        declaredClass = method.declaringClass.name
                        name = method.name
                    }
                }
            }
        }
    }

    private val changedField by lazy {
        val method = addMethod ?: return@lazy null
        optionalMember("DockEditChangedFieldV2") {
            it.findField {
                matcher {
                    declaredClass = method.declaringClass.name
                    type = "boolean"
                    addWriteMethod {
                        declaredClass = method.declaringClass.name
                        name = method.name
                    }
                }
            }.singleOrNull()
        } as? Field
    }

    override fun initDexKit(): Boolean {
        var ok = true
        if (editActivityClassFromDex == null) {
            XposedLog.w(TAG, packageName, "dexkit miss: editActivityClass")
            ok = false
        }
        if (addMethod == null) {
            XposedLog.w(TAG, packageName, "dexkit miss: addMethod")
            ok = false
        }
        if (removeMethod == null) {
            XposedLog.w(TAG, packageName, "dexkit miss: removeMethod")
        }
        if (changedField == null) {
            XposedLog.w(TAG, packageName, "dexkit miss: changedField")
        }
        if (sourceCollectMethod == null) {
            XposedLog.w(TAG, packageName, "dexkit miss: sourceCollectMethod")
        }
        if (sourceListFields.isEmpty()) {
            XposedLog.w(TAG, packageName, "dexkit miss: sourceListFields")
        }
        return ok
    }

    override fun init() {
        initEditClasses()
        hookSourceLimit()
        hookEditActivity()
        hookEditPage()
    }

    private fun hookEditPage() {
        hookSelectedAdapter()
        hookAllAdapter()
    }

    private fun hookSelectedAdapter() {
        val adapterClass = editSelectedAdapterClass ?: return
        Constructors.find(adapterClass).toList().createAfterHooks {
            updateSelectedCount(it.thisObject)
        }

        adapterClass.findMethod { name("getItemCount") }.createHook {
            before {
                it.result = limit
            }
        }

        findSelectedItemMethods().createAfterHooks {
            updateSelectedCount(it.thisObject)
        }
    }

    private fun hookAllAdapter() {
        findAllAdapterEnableMethod()?.createHook {
            before {
                it.args[0] = adjustedAddEnabled(it.args[0])
            }
        }
    }

    private fun hookEditActivity() {
        val activityClass = editActivityClass ?: return
        activityClass.findMethod {
            name("onCreate")
            parameterTypes(Bundle::class.java)
        }.createHook {
            after {
                updateDesc(it.thisObject as Activity)
            }
        }

        addMethod?.createHook {
            before {
                handleAddLimitBefore(it)
            }
            after {
                try {
                    restoreWrappedLists(it.thisObject, wrappedEditLists)
                } finally {
                    updateAllAdapterWhenFull(it)
                }
            }
        }

        removeMethod?.createHook {
            after {
                updateAllAdapterAfterRemove(it.thisObject)
            }
        }
    }

    private fun hookSourceLimit() {
        sourceCollectMethod?.createHook {
            before {
                if (hideSuggest) {
                    it.result = null
                    return@before
                }
                wrapSourceLists(it.thisObject)
            }
            after {
                if (hideSuggest) return@after
                restoreWrappedLists(it.thisObject, wrappedSourceLists)
            }
        } ?: XposedLog.w(TAG, packageName, "source collect method not found")
    }

    private fun initEditClasses() {
        val activityClass = editActivityClassFromDex ?: addMethod?.declaringClass ?: return
        editActivityClass = activityClass
        editItemClass = addMethod?.parameterTypes?.getOrNull(2)
        editAllAdapterClass = addMethod?.parameterTypes?.getOrNull(0)
        editSelectedAdapterClass = findSelectedAdapterClass()
    }

    private fun findSelectedAdapterClass(): Class<*>? {
        val activityClass = editActivityClass ?: return null
        val adapterClass = editAllAdapterClass?.findAdapterSuperclass() ?: return null
        return activityClass.findFieldOrNull {
            findOnlyClass()
            typeExtendsFrom(adapterClass)
        }?.type
    }

    private fun Class<*>.findAdapterSuperclass(): Class<*>? {
        return firstSuperclass {
            hasDeclaredMethod("bindViewHolder", 2) && hasDeclaredMethod("notifyItemRangeChanged", 3)
        }
    }

    private fun Class<*>.firstSuperclass(predicate: Class<*>.() -> Boolean): Class<*>? {
        return generateSequence<Class<*>>(superclass) { it.superclass }
            .takeWhile { it != Any::class.java }
            .find { it.predicate() }
    }

    private fun Class<*>.hasDeclaredMethod(methodName: String, parameterCount: Int): Boolean {
        return findMethodOrNull {
            findOnlyClass()
            name(methodName)
            paramCount(parameterCount)
        } != null
    }

    private fun findSelectedItemMethods(): List<Method> {
        val adapterClass = editSelectedAdapterClass ?: return emptyList()
        val itemClass = editItemClass ?: return emptyList()
        return adapterClass.findAllMethods {
            voidReturnType()
            params(itemClass)
        }
    }

    private fun findAllAdapterEnableMethod(): Method? {
        val adapterClass = editAllAdapterClass ?: return null
        return adapterClass.findMethodOrNull {
            voidReturnType()
            params(Boolean::class.javaPrimitiveType!!)
        }
    }

    private fun handleAddLimitBefore(param: HookParam) {
        val activity = param.thisObject
        refreshSelectedCount(activity)
        if (selectedCount >= limit) {
            param.result = null
            return
        }
        if (selectedCount >= DEFAULT_LIMIT) {
            changedField?.let { activity.setBooleanField(it.name, true) }
            wrapEditLists(activity)
        }
    }

    private fun adjustedAddEnabled(value: Any?): Any? {
        return when {
            selectedCount >= limit -> false
            value == false -> true
            else -> value
        }
    }

    private fun updateAllAdapterWhenFull(param: HookParam) {
        try {
            val allAdapter = param.args.getOrNull(0) ?: return
            refreshSelectedCount(param.thisObject)
            if (selectedCount < limit) return
            findAllAdapterEnableMethod()?.invoke(allAdapter, false)
            allAdapter.notifyIconRangeChanged()
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "update all adapter state failed", t)
        }
    }

    private fun updateAllAdapterAfterRemove(activity: Any) {
        try {
            refreshSelectedCount(activity)
            if (selectedCount >= limit) return
            val allAdapter = activity.allAdapter() ?: return
            findAllAdapterEnableMethod()?.invoke(allAdapter, true)
            allAdapter.notifyIconRangeChanged()
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "restore all adapter state failed", t)
        }
    }

    private fun updateDesc(activity: Activity) {
        try {
            val descId = activity.getIdByName("gd_app_edit_desc_new", "plurals")
            val textView = activity.findViewByIdName("tv_desc") as? TextView ?: return
            if (descId != 0) {
                textView.text = activity.resources.getQuantityString(descId, limit, limit)
            }
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "update edit description failed", t)
        }
    }

    private fun updateSelectedCount(selectedAdapter: Any) {
        selectedCount = selectedAdapter.selectedAdapterCount()
    }

    private fun refreshSelectedCount(activity: Any) {
        selectedCount = activity.selectedAdapter()?.selectedAdapterCount()
            ?: activity.selectedListFromFields()?.size
            ?: selectedCount
    }

    private fun Any.selectedAdapter(): Any? {
        return adapterValue(editSelectedAdapterClass)
    }

    private fun Any.allAdapter(): Any? {
        return adapterValue(editAllAdapterClass)
    }

    private fun Any.adapterValue(adapterClass: Class<*>?): Any? {
        adapterClass ?: return null
        val activityClass = editActivityClass ?: return null
        return activityClass.findFieldOrNull {
            type(adapterClass)
        }?.get(this)
    }

    private fun Any.selectedListFromFields(): List<*>? {
        val activityClass = editActivityClass ?: return null
        return activityClass.findAllFields {
            findOnlyClass()
            type(List::class.java)
        }.firstNotNullOfOrNull { field ->
            field.isAccessible = true
            field.get(this) as? List<*>
        }
    }

    private fun Any.selectedAdapterCount(): Int {
        val list = selectedList() ?: return selectedCount
        return (list as? ActualSizeList)?.actualSize ?: list.size
    }

    private fun Any.selectedList(): List<*>? {
        // EzHookTool 1.1.0+ 会按结构化条件缓存结果，无需在此手动缓存 Method 引用
        val method = findSelectedListMethod() ?: return null
        return method.invoke(this) as? List<*>
    }

    private fun findSelectedListMethod(): Method? {
        val adapterClass = editSelectedAdapterClass ?: return null
        return adapterClass.findMethodOrNull {
            returnType(List::class.java)
            noParams()
        }
    }

    private fun Any.notifyIconRangeChanged() {
        val itemCount = javaClass.findMethodOrNull {
            name("getItemCount")
            noParams()
        }?.invoke(this) as? Int ?: return
        javaClass.findMethodOrNull {
            name("notifyItemRangeChanged")
            paramCount(3)
        }?.invoke(this, 0, itemCount, "icon")
    }

    private fun wrapSourceLists(owner: Any) {
        wrapListFields(
            owner = owner,
            fields = sourceListFields,
            cache = wrappedSourceLists,
            emptyMessage = "no source list field wrapped",
        ) { list -> WrappedLimitList(list, limit, DEFAULT_SOURCE_LIMIT, WrappedLimitList.Mode.SOURCE) }
    }

    private fun wrapEditLists(owner: Any) {
        // 进入扩展上限路径：用户已选数量已经达到 stock 上限 (10)，
        // 但用户开启了更大的 limit。包装 list 的 size() 让宿主代码里
        // `size() == 10` / `size() >= 10` 的临界判断仍保持原有行为，
        // 避免提前触发 "已满" 分支。
        if (limit == DEFAULT_LIMIT) return
        val activityClass = editActivityClass ?: return
        val fields = activityClass.findAllFields {
            findOnlyClass()
            type(List::class.java)
        }
        wrapListFields(
            owner = owner,
            fields = fields,
            cache = wrappedEditLists,
            emptyMessage = "no edit list field wrapped",
            accept = { it.size in DEFAULT_LIMIT..<limit },
        ) { list -> WrappedLimitList(list, limit, DEFAULT_LIMIT, WrappedLimitList.Mode.EDIT) }
    }

    private inline fun wrapListFields(
        owner: Any,
        fields: Iterable<Field>,
        cache: WeakHashMap<Any, List<Pair<Field, Any?>>>,
        emptyMessage: String,
        accept: (MutableList<Any?>) -> Boolean = { true },
        wrap: (MutableList<Any?>) -> MutableList<Any?>,
    ) {
        val originals = ArrayList<Pair<Field, Any?>>()
        for (field in fields) {
            field.isAccessible = true
            val value = field.get(owner)
            @Suppress("UNCHECKED_CAST")
            val list = value as? MutableList<Any?> ?: continue
            if (!accept(list)) continue
            field.set(owner, wrap(list))
            originals.add(field to value)
        }
        if (originals.isNotEmpty()) {
            cache[owner] = originals
        } else {
            XposedLog.w(TAG, packageName, emptyMessage)
        }
    }

    private fun restoreWrappedLists(
        owner: Any,
        cache: WeakHashMap<Any, List<Pair<Field, Any?>>>
    ) {
        cache.remove(owner)?.forEach { (field, value) ->
            field.set(owner, value)
        }
    }

    private interface ActualSizeList {
        val actualSize: Int
    }

    /**
     * 包装 list 让其 `size()` 在临界值时返回 `stockLimit - 1`，欺骗宿主代码的 `==` 或 `>=` 判定。
     *
     * - [EDIT] 用于 edit 页面已选 list：只要 delegate 已经达到 stockLimit，就持续报告 stockLimit-1，
     *   避免在扩展上限路径中触发 "已满" 分支。
     * - [SOURCE] 用于源推荐池：仅在 delegate 恰好等于 stockLimit 时报告 stockLimit-1，超过则
     *   直接报告 stockLimit（贴近原行为，只阻断"即将装满"的那一帧）。
     */
    private class WrappedLimitList<T>(
        private val delegate: MutableList<T>,
        private val limit: Int,
        private val stockLimit: Int,
        private val mode: Mode,
    ) : MutableList<T> by delegate, ActualSizeList {
        enum class Mode { EDIT, SOURCE }

        override val actualSize: Int
            get() = delegate.size

        override val size: Int
            get() {
                val real = delegate.size
                return when {
                    real >= limit -> stockLimit
                    mode == Mode.EDIT && real >= stockLimit -> stockLimit - 1
                    mode == Mode.SOURCE && real == stockLimit -> stockLimit - 1
                    else -> real
                }
            }
    }

    private companion object {
        const val DEFAULT_LIMIT = 10
        const val DEFAULT_SOURCE_LIMIT = 30
    }
}
