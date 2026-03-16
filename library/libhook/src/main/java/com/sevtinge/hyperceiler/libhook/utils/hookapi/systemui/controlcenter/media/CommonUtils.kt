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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media

import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.clzConstraintSetClass
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.mediaViewHolderNew
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewHolder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

object ConstraintSetHelper {
    val clear by lazy {
        clzConstraintSetClass!!.methodFinder()
            .filterByName("clear")
            .filterByParamTypes(Int::class.java, Int::class.java)
            .first()
    }
    val setVisibility by lazy {
        clzConstraintSetClass!!.methodFinder()
            .filterByName("setVisibility")
            .filterByParamTypes(Int::class.java, Int::class.java)
            .first()
    }
    val connect by lazy {
        clzConstraintSetClass!!.methodFinder()
            .filterByName("connect")
            .filterByParamTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java)
            .first()
    }
    val setMargin by lazy {
        clzConstraintSetClass!!.methodFinder()
            .filterByName("setMargin")
            .filterByParamTypes(Int::class.java, Int::class.java, Int::class.java)
            .first()
    }
    val setGoneMargin by lazy {
        clzConstraintSetClass!!.methodFinder()
            .filterByName("setGoneMargin")
            .filterByParamTypes(Int::class.java, Int::class.java, Int::class.java)
            .first()
    }
    val applyTo by lazy {
        clzConstraintSetClass!!.methodFinder()
            .filterByName("applyTo")
            .first()
    }
    val clone by lazy {
        clzConstraintSetClass!!.methodFinder()
            .filterByName("clone")
            .first()
    }
}

// 向后兼容的顶层访问器，委托到 ConstraintSetHelper
val clear get() = ConstraintSetHelper.clear
val setVisibility get() = ConstraintSetHelper.setVisibility
val connect get() = ConstraintSetHelper.connect
val setMargin get() = ConstraintSetHelper.setMargin
val setGoneMargin get() = ConstraintSetHelper.setGoneMargin
val applyTo get() = ConstraintSetHelper.applyTo
val clone get() = ConstraintSetHelper.clone

private val ncMediaVHFieldCache = ConcurrentHashMap<String, Field>()
private val diMediaVHFieldCache = ConcurrentHashMap<String, Field>()

@Suppress("UNCHECKED_CAST")
fun <T> Any.getMediaViewHolderFieldAs(fieldName: String, isDynamicIsland: Boolean): T? {
    val cache = if (isDynamicIsland) diMediaVHFieldCache else ncMediaVHFieldCache
    val holderClass = if (isDynamicIsland) miuiIslandMediaViewHolder else mediaViewHolderNew
    val field = cache.getOrPut(fieldName) {
        holderClass?.let { findField(it, fieldName) }
    }
    return field?.get(this) as? T
}
