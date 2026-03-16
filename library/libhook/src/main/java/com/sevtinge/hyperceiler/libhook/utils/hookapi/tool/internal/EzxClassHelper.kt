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

import io.github.kyuubiran.ezxhelper.core.ClassLoaderProvider.safeClassLoader

/**
 * 类查找工具
 * 封装类名查找逻辑，支持内部类回退（"a.b.C.D" 和 "a.b.C$D" 两种表示法）
 */
internal object EzxClassHelper {

    fun findClass(name: String): Class<*> {
        return findClassInternal(name, safeClassLoader)
            ?: throw ClassNotFoundException("Class not found: $name")
    }

    fun findClass(name: String, classLoader: ClassLoader?): Class<*> {
        val cl = classLoader ?: safeClassLoader
        return findClassInternal(name, cl)
            ?: throw ClassNotFoundException("Class not found: $name")
    }

    fun findClassIfExists(name: String): Class<*>? {
        return findClassInternal(name, safeClassLoader)
    }

    fun findClassIfExists(name: String, classLoader: ClassLoader?): Class<*>? {
        val cl = classLoader ?: safeClassLoader
        return findClassInternal(name, cl)
    }

    /**
     * 带有内部类回退的类查找。
     * 支持 "a.b.C.D" 和 "a.b.C$D" 两种内部类表示法
     */
    private fun findClassInternal(name: String, classLoader: ClassLoader): Class<*>? {
        // 直接查找
        try {
            return Class.forName(name, false, classLoader)
        } catch (_: ClassNotFoundException) {}

        // 内部类回退：从右到左将 '.' 替换为 '$'
        val chars = name.toCharArray()
        for (i in chars.indices.reversed()) {
            if (chars[i] == '.') {
                chars[i] = '$'
                try {
                    return Class.forName(String(chars), false, classLoader)
                } catch (_: ClassNotFoundException) {}
            }
        }
        return null
    }
}
