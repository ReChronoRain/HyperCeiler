/*
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile

import java.lang.reflect.Method

/** 缓存宿主反射入口，始终读取最新颜色；不跨 classloader 强转 Kotlin Triple。 */
internal class MobileTintSource private constructor(
    private val flow: Any,
    private val getValue: Method,
    tripleClass: Class<*>
) {
    private val first = tripleClass.getMethod("getFirst")
    private val second = tripleClass.getMethod("getSecond")
    private val third = tripleClass.getMethod("getThird")

    fun read(): DarkInfo {
        val value = getValue.invoke(flow)
        val useTint = first.invoke(value) as? Boolean ?: error("Invalid tint flag")
        val lightIcons = second.invoke(value) as? Boolean ?: error("Invalid light icon flag")
        val color = third.invoke(value) as? Int ?: error("Invalid icon color")
        return DarkInfo.fromTintLightColor(useTint, lightIcons, color)
    }

    companion object {
        fun find(binding: Any): MobileTintSource? {
            for (field in binding.javaClass.declaredFields) {
                val source = runCatching {
                    field.isAccessible = true
                    val flow = field.get(binding) ?: return@runCatching null
                    if (flow.javaClass.simpleName != "StateFlowImpl") return@runCatching null
                    val getter = flow.javaClass.getMethod("getValue").apply { isAccessible = true }
                    val value = getter.invoke(flow) ?: return@runCatching null
                    if (value.javaClass.name != "kotlin.Triple") return@runCatching null
                    MobileTintSource(flow, getter, value.javaClass).also { it.read() }
                }.getOrNull()
                if (source != null) return source
            }
            return null
        }
    }
}
