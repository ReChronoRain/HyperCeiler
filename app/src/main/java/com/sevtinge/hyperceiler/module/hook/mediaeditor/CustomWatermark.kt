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
package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

object CustomWatermark : BaseHook() {
    private val name by lazy {
        mPrefsMap.getString("mediaeditor_custom_watermark", "")
    }

    override fun init(){
        // by StarVoyager
        val search = dexKitBridge.findMethod {
            matcher {
                addUsingString("K30 Pro Zoom E", StringMatchType.Equals)
                modifiers = Modifier.FINAL
                returnType = "java.lang.String"
                paramCount = 2
            }
        }.single().getMethodInstance(EzXHelper.classLoader)

        logE(TAG, "[CustomWatermark] search method is $search")
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
