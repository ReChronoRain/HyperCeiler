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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import org.luckypray.dexkit.query.enums.*
import java.lang.reflect.*

object CustomWatermark : BaseHook() {
    private val name by lazy {
        mPrefsMap.getString("mediaeditor_custom_watermark", "")
    }

    // by StarVoyager
    private val search by lazy<Method> {
        DexKit.findMember("CustomWatermark") {
            it.findMethod {
                matcher {
                    addUsingString("K30 Pro Zoom E", StringMatchType.Equals)
                    // modifiers = Modifier.FINAL // 1.6.5.10.2 改成了 STATIC，所以寄了
                    returnType = "java.lang.String"
                    paramCount = 2
                }
            }.single()
        }
    }

    override fun init() {
        logD(TAG, lpparam.packageName, "[CustomWatermark] search method is $search")
        search.createHook {
            // 当前只能修改后缀
            returnConstant(name)
        }

        /*SystemProperties.methodFinder()
            .filterByParamCount(2)
            .filterByParamTypes(String::class.java, String::class.java)
            .toList().createHooks {
                before {
                    if (it.args[0] == "ro.product.marketname") {
                        it.args[1] = name
                    }
                }
            }

        SystemProperties.methodFinder()
            .filterByName("get")
            .filterByParamTypes(String::class.java)
            .toList().createHooks {
                before {
                    if (it.args[0] == "ro.product.marketname") {
                        it.result = name
                    }
                }
            }*/
    }
}
