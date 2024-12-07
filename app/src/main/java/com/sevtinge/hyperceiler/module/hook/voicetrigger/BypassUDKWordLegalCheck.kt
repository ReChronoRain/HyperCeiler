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
package com.sevtinge.hyperceiler.module.hook.voicetrigger

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import java.lang.reflect.*

object BypassUDKWordLegalCheck : BaseHook() {
    override fun init() {
        try {
            // Pangaea引擎录入时的联网检查
            DexKit.findMember("BypassPangaeaWordCheck") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals("PangaeaTrainingSession", "onlineQuery=")
                        returnType = "java.lang.Boolean"
                    }
                }.single().getMethodInstance(lpparam.classLoader)
            }.toMethod().createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
        // 默认引擎录入时的联网检查
        try {
            DexKit.findMember("BypassLegacyTrainingCheck") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals("LegacyTrainingSession", "onlineQuery=")
                        returnType = "java.lang.Boolean"
                    }
                }.single().getMethodInstance(lpparam.classLoader)
            }.toMethod().createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
        // 判断唤醒词是否合规
        try {
            DexKit.findMember("BypassDefineWordCheck") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals(
                            "https://i.ai.mi.com/api/skillstore/assistant/store/visitors/checkWakeUpWord",
                            "NetUtils",
                            "checkUDKWordLegal, callResult="
                        )
                        returnType = "boolean"
                    }
                }.single().getMethodInstance(lpparam.classLoader)
            }.toMethod().createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
        // 根据对应的唤醒词得到其精度，并返回其是否可用
        val accUser = mPrefsMap.getInt("voicetrigger_accuracy_percent", 70).toFloat() / 100
        try {
            DexKit.findMember("BypassOnlineAccuracyResult") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals(
                            "https://i.ai.mi.com/api/skillstore/assistant/store/visitors/checkWakeUpWord",
                            "NetUtils",
                            "getUDKEnrollWordLegal can't get result"
                        )
                        returnType = "java.lang.String"
                    }
                }.single().getMethodInstance(lpparam.classLoader)
            }.toMethod().createHook {
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
            DexKit.findMember("BypassNetworkStateCheckForUdkEnroll") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals("connectivity")
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
                }.single().getMethodInstance(lpparam.classLoader)
            }.toMethod().createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
    }
}
