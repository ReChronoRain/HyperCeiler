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
package com.sevtinge.hyperceiler.module.hook.voicetrigger;

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import java.lang.reflect.*

object BypassUDKWordLegalCheck : BaseHook() {
    override fun init() {
        try {
            DexKit.getDexKitBridge("BypassUDKWordLegalCheck") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals("PangaeaTrainingSession", "onlineQuery=")
                        returnType = "java.lang.Boolean"
                    }
                }.single().getMethodInstance(lpparam.classLoader)
            }.toMethod().createHook {
                returnConstant(true)
            }
        } catch (e: Throwable) {
        }

        try {
            DexKit.getDexKitBridge("BypassUDKWordLegalCheck1") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals("LegacyTrainingSession", "onlineQuery=")
                        returnType = "java.lang.Boolean"
                    }
                }.single().getMethodInstance(lpparam.classLoader)
            }.toMethod().createHook {
                returnConstant(true)
            }
        } catch (e: Throwable) {
        }
        try {
            DexKit.getDexKitBridge("BypassUDKWordLegalCheck2") {
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
        } catch (e: Throwable) {
        }
        try {
            DexKit.getDexKitBridge("BypassUDKWordLegalCheck3") {
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
                returnConstant("{\"data\":{\"status\":0,\"msg\":\"\"}}")
            }
        } catch (e: Throwable) {
        }
        try {
            DexKit.getDexKitBridge("BypassUDKWordLegalCheck4") {
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
        } catch (e: Throwable) {
        }
    }
}
