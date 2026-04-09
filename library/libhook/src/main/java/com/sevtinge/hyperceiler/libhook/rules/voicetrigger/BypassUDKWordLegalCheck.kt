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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.voicetrigger

import android.annotation.SuppressLint
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object BypassUDKWordLegalCheck : BaseHook() {
    override fun useDexKit() = true
    private var bypassPangaeaWordCheck: Method? = null
    private var bypassLegacyTrainingCheck: Method? = null
    private var bypassDefineWordCheck: Method? = null
    private var bypassOnlineAccuracyResult: Method? = null
    private var bypassNetworkStateCheckForUdkEnroll: Method? = null

    override fun initDexKit(): Boolean {
        bypassPangaeaWordCheck = optionalMember("BypassPangaeaWordCheck") {
            it.findMethod {
                matcher {
                    usingEqStrings("PangaeaTrainingSession", "onlineQuery=")
                    returnType = "java.lang.Boolean"
                }
            }.single()
        }
        bypassLegacyTrainingCheck = optionalMember("BypassLegacyTrainingCheck") {
            it.findMethod {
                matcher {
                    usingEqStrings("LegacyTrainingSession", "onlineQuery=")
                    returnType = "java.lang.Boolean"
                }
            }.single()
        }
        bypassDefineWordCheck = optionalMember("BypassDefineWordCheck") {
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
        }
        bypassOnlineAccuracyResult = optionalMember("BypassOnlineAccuracyResult") {
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
        }
        bypassNetworkStateCheckForUdkEnroll = optionalMember("BypassNetworkStateCheckForUdkEnroll") {
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
        }
        return true
    }

    @SuppressLint("DefaultLocale")
    override fun init() {
        runCatching {
            // Pangaea 引擎录入时的联网检查
            bypassPangaeaWordCheck?.createHook {
                returnConstant(true)
            }
        }
        // 默认引擎录入时的联网检查
        runCatching {
            bypassLegacyTrainingCheck?.createHook {
                returnConstant(true)
            }
        }
        // 判断唤醒词是否合规
        runCatching {
            bypassDefineWordCheck?.createHook {
                returnConstant(true)
            }
        }
        // 根据对应的唤醒词得到其精度，并返回其是否可用
        val accUser = PrefsBridge.getInt("voicetrigger_accuracy_percent", 70).toFloat() / 100
        runCatching {
            bypassOnlineAccuracyResult?.createHook {
                returnConstant(
                    "{\"data\":{\"status\":0,\"msg\":\"\",\"accuracy\":\"" + String.format(
                        "%.1f",
                        accUser
                    ) + "\"}}"
                )
            }
        }
        runCatching {
            // 禁止判断当前系统网络状态
            bypassNetworkStateCheckForUdkEnroll?.createHook {
                returnConstant(true)
            }
        }
    }
}
