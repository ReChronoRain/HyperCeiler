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
package com.sevtinge.hyperceiler.hook.module.rules.mediaeditor

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

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
                    // paramCount = 2 // 2.0.9.0.6 多了一个 boolean 参数
                }
            }.single()
        }
    }

    private val searchNew by lazy<Method> {
        // 2.2.0.1.6 开始混淆类名和字符串
        DexKit.findMember("CustomWatermarkNew") {
            it.findClass {
                matcher {
                    addUsingString("check geocode selectable", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    addInvoke("Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;")
                    returnType = "java.lang.String"
                }
            }.single()
        }
    }

    override fun init() {
        runCatching {
            search.createHook {
                returnConstant(name)
            }
        }.onFailure {
            searchNew.createHook {
                returnConstant(name)
            }
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
