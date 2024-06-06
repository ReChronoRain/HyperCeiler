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
package com.sevtinge.hyperceiler.module.hook.updater

import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import com.sevtinge.hyperceiler.utils.*
import org.luckypray.dexkit.query.enums.*

object AutoUpdateDialog : BaseHook() {
    private val find1 by lazy {
        DexKit.getDexKitBridge("AutoUpdateDialog1") {
            it.findMethod {
                matcher {
                    addCall {
                        addUsingString("isShowAutoSetDialog", StringMatchType.Contains)
                    }
                    paramTypes("boolean", "boolean")
                }
            }.single().getMethodInstance(EzXHelper.safeClassLoader)
        }.toMethod()
    }

    private val find2 by lazy {
        DexKit.getDexKitBridge("AutoUpdateDialog2") {
            it.findMethod {
                matcher {
                    addCall {
                        addUsingString("isShowMobileDownloadDialog", StringMatchType.Contains)
                    }
                    paramTypes("long", "int")
                }
            }.single().getMethodInstance(EzXHelper.safeClassLoader)
        }.toMethod()
    }

    override fun init() {
        logD(TAG, lpparam.packageName, "get find1 is $find1")
        logD(TAG, lpparam.packageName, "get find2 is $find2")
        setOf(find1, find2).forEach {
            it.replaceMethod { 0 }
        }
    }
}
