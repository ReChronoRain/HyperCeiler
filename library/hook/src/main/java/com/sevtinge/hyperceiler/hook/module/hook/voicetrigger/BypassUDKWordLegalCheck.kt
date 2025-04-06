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
package com.sevtinge.hyperceiler.hook.module.hook.voicetrigger

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import java.lang.reflect.*

object BypassUDKWordLegalCheck : BaseHook() {
    override fun init() {
        try {
            // Pangaea引擎录入时的联网检查
            DexKit.findMember<Method>("BypassPangaeaWordCheck") {
                it.findMethod {
                    matcher {
                        usingEqStrings("PangaeaTrainingSession", "onlineQuery=")
                        returnType = "java.lang.Boolean"
                    }
                }.single()
            }.createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
        // 默认引擎录入时的联网检查
        try {
            DexKit.findMember<Method>("BypassLegacyTrainingCheck") {
                it.findMethod {
                    matcher {
                        usingEqStrings("LegacyTrainingSession", "onlineQuery=")
                        returnType = "java.lang.Boolean"
                    }
                }.single()
            }.createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
        // 判断唤醒词是否合规
        try {
            DexKit.findMember<Method>("BypassDefineWordCheck") {
                it.findMethod {
                    matcher {
                        usingEqStrings(
                            "https://i.ai.mi.com/api/skillstore/assistant/store/visitors/checkWakeUpWord",
                            "NetUtils",
                            "checkUDKWordLegal, callResult="
                        )
                        returnType = "boolean"
                    }
                }.single()
            }.createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
        // 根据对应的唤醒词得到其精度，并返回其是否可用
        val accUser = mPrefsMap.getInt("voicetrigger_accuracy_percent", 70).toFloat() / 100
        try {
            DexKit.findMember<Method>("BypassOnlineAccuracyResult") {
                it.findMethod {
                    matcher {
                        usingEqStrings(
                            "https://i.ai.mi.com/api/skillstore/assistant/store/visitors/checkWakeUpWord",
                            "NetUtils",
                            "getUDKEnrollWordLegal can't get result"
                        )
                        returnType = "java.lang.String"
                    }
                }.single()
            }.createHook {
                returnConstant(
                    "{\"data\":{\"status\":0,\"msg\":\"\",\"accuracy\":\"" + String.format(
                        "%.1f",
                        accUser
                    ) + "\"}}"
                )
            }
        } catch (_: Throwable) {
        }
        try {
            // 禁止判断当前系统网络状态
            DexKit.findMember<Method>("BypassNetworkStateCheckForUdkEnroll") {
                it.findMethod {
                    matcher {
                        usingEqStrings("connectivity")
                        paramCount = 1
                        paramTypes("android.content.Context")
                        modifiers = Modifier.STATIC
                        returnType = "boolean"
                        opNames = listOf(
                            "const-string",
                            "invoke-virtual",
                            "move-result-object",
                            "check-cast",
                            "const/4",
                            "if-nez",
                            "return",
                            "invoke-virtual"
                        )

                    }
                }.single()
            }.createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
    }
}
