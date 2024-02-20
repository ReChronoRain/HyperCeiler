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

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher

/**
 * DexKit 工具
 */
object DexKit {
    var hostDir: String? = null
    var isInitialized = false
    val dexKitBridge: DexKitBridge by lazy {
        InitDexKit.init()
    }

    /**
     * 初始化 DexKit 的 apk 完整路径
     */
    fun initDexKit(loadPackageParam: LoadPackageParam) {
        hostDir = loadPackageParam.appInfo.sourceDir
    }

    /**
     * 关闭 DexKit bridge
     */
    fun closeDexKit() {
        if (isInitialized) dexKitBridge.close()
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
