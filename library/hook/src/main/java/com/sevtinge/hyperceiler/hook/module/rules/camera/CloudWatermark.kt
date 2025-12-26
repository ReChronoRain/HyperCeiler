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
package com.sevtinge.hyperceiler.hook.module.rules.camera

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.json.JSONObject
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method
import java.lang.reflect.Modifier

// thank HolyBear
object CloudWatermark : BaseHook() {

    private val cloudMethod by lazy {
        // 仅支持 6.2 及以上版本，用于强制获取云下发的新水印内容
        DexKit.findMember("cloud") {
            it.findMethod {
                searchPackages("com.xiaomi.camera")
                matcher {
                    addInvoke {
                        modifiers = Modifier.FINAL
                        paramTypes(Long::class.java)
                        returnType = "boolean"
                    }
                    returnType = "boolean"
                }
            }.single()
        } as Method?
    }

    private val cloudDelete by lazy {
        // 仅支持 6.2 及以上版本，用于强制获取云下发的新水印内容
        DexKit.findMember("deleteCloud") {
            it.findClass {
                matcher {
                    addUsingString("watermark_enable", StringMatchType.Equals)
                }
            }.findMethod {
                matcher {
                    addInvoke("Ljava/util/List;->isEmpty()Z")
                    returnType = "void"
                }
            }.single()
        } as Method?
    }

    override fun init() {
        if (cloudMethod == null) {
            logD(TAG, lpparam.packageName, "maybe not support this version")
            return
        }

        cloudMethod?.createHook {
            returnConstant(true)
        }

        runCatching {
            cloudDelete?.createHook {
                before {
                    it.result = null
                }
            }
        }.onFailure {
            logW(TAG, lpparam.packageName, "hook deleteCloud failed, maybe not support this version")
        }

        JSONObject::class.java.methodFinder()
            .filterByName("optJSONObject")
            .first()
            .createHook {
                before {
                    // 忽略时间和机型限制
                    val limitation = it.args[0] as String
                    if (limitation.contains("limitation")) {
                        logD(TAG, lpparam.packageName, "block limitation optJSONObject")
                        it.result = null
                    }
                }
            }
    }
}
