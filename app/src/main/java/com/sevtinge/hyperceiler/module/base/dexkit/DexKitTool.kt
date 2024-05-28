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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.base.dexkit

import org.luckypray.dexkit.query.enums.*
import org.luckypray.dexkit.query.matchers.*
import org.luckypray.dexkit.result.*
import java.lang.reflect.*

/**
 * DexKit 工具
 */
object DexKitTool {
    /**
     * 将 ClassData 列表快捷转为 List<AnnotatedElement>
     * 使用时在查找后调用 .toElementList(EzXHelper.safeClassLoader) 即可
     */
    fun ClassDataList.toElementList(classLoader: ClassLoader): List<AnnotatedElement> {
        return DexKit.toElementList(this, classLoader)
    }

    /**
     * 将 MethodData 列表快捷转为 List<AnnotatedElement>
     * 使用时在查找后调用 .toElementList(EzXHelper.safeClassLoader) 即可
     */
    fun MethodDataList.toElementList(classLoader: ClassLoader): List<AnnotatedElement> {
        return DexKit.toElementList(this, classLoader)
    }

    /**
     * 将 FieldData 列表快捷转为 List<AnnotatedElement>
     * 使用时在查找后调用 .toElementList(EzXHelper.safeClassLoader) 即可
     */
    fun FieldDataList.toElementList(classLoader: ClassLoader): List<AnnotatedElement> {
        return DexKit.toElementList(this, classLoader)
    }

    /**
     * 快捷转为 MethodDataList
     */
    fun BaseDataList<*>.toMethodDataList(): MethodDataList {
        return this as MethodDataList
    }

    /**
     * 快捷转为 FieldDataList
     */
    fun BaseDataList<*>.toFieldDataList(): FieldDataList {
        return this as FieldDataList
    }

    /**
     * 快捷转为 ClassDataList
     */
    fun BaseDataList<*>.toClassDataList(): ClassDataList {
        return this as ClassDataList
    }

    /**
     * 快捷类型转换为 Method
     */
    fun AnnotatedElement.toMethod(): Method {
        return this as Method
    }

    /**
     * 快捷类型转换为 Field
     */
    fun AnnotatedElement.toField(): Field {
        return this as Field
    }

    /**
     * 快捷类型转换为 Class
     */
    fun AnnotatedElement.toClass(): Class<*> {
        return this as Class<*>
    }

    /**
     * 快捷类型转换为 Constructor
     */
    fun AnnotatedElement.toConstructor(): Constructor<*> {
        return this as Constructor<*>
    }

    /**
     * DexKit 封装查找方式
     */
    fun MethodMatcher.addUsingStringsEquals(vararg strings: String) {
        for (string in strings) {
            addUsingString(string, StringMatchType.Equals)
        }
    }

    fun ClassMatcher.addUsingStringsEquals(vararg strings: String) {
        for (string in strings) {
            addUsingString(string, StringMatchType.Equals)
        }
    }
}
