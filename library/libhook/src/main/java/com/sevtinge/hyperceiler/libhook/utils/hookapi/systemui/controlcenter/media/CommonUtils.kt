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
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

object ConstraintSetHelper {
    val clear by lazy {
        clzConstraintSetClass!!.findMethod { name("clear"); parameterTypes(Int::class.java, Int::class.java) }
    }
    val setVisibility by lazy {
        clzConstraintSetClass!!.findMethod { name("setVisibility"); parameterTypes(Int::class.java, Int::class.java) }
    }
    val connect by lazy {
        clzConstraintSetClass!!.findMethod { name("connect"); parameterTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java) }
    }
    val setMargin by lazy {
        clzConstraintSetClass!!.findMethod { name("setMargin"); parameterTypes(Int::class.java, Int::class.java, Int::class.java) }
    }
    val setGoneMargin by lazy {
        clzConstraintSetClass!!.findMethod { name("setGoneMargin"); parameterTypes(Int::class.java, Int::class.java, Int::class.java) }
    }
    val applyTo by lazy {
        clzConstraintSetClass!!.findMethod { name("applyTo") }
    }
    val clone by lazy {
        clzConstraintSetClass!!.findMethod { name("clone") }
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
        generateSequence(holderClass) { it.superclass }
            .mapNotNull { currentClass ->
                runCatching {
                    currentClass.getDeclaredField(fieldName).apply { isAccessible = true }
                }.getOrNull()
            }
            .firstOrNull() ?: error("Field $fieldName not found in ${holderClass?.name}")
    }
    return field?.get(this) as? T
}
