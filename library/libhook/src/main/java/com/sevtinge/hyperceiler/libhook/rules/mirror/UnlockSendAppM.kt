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
package com.sevtinge.hyperceiler.libhook.rules.mirror

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object UnlockSendAppM : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        miCloudUtils
        whitelist
        supportAllAppSubScreenMethod
        return true
    }

    private val supportAllAppSubScreenMethod by lazy {
        optionalMember("supportAllAppSubScreen") { bridge ->
            bridge.findMethod {
                matcher {
                    addUsingString("support_all_app_sub_screen", StringMatchType.Equals)
                    returnType = "boolean"
                }
            }.single()
        } as? Method
    }

    private val miCloudUtils by lazy<List<Method>> {
        optionalMemberList("MiCloudUtils") { bridge ->
            bridge.findMethod {
                matcher {
                    addUsingString("MiCloudUtils", StringMatchType.Equals)
                    returnType = "boolean"
                }
            }
        }
    }

    private val whitelist by lazy {
        optionalMember("Whitelist") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass {
                        addUsingString("MiCloudUtils", StringMatchType.Equals)
                    }
                    addInvoke("Ljava/lang/Long;->longValue()J")
                    paramCount = 1
                    returnType = "boolean"
                }
            }.single()
        } as? Method
    }

    override fun init() {
        hookRelayApplicationGetters($$"com.xiaomi.mirror.message.proto.RelayApp$RelayApplication")
        hookRelayApplicationGetters($$"com.xiaomi.mirror.message.global.proto.RelayApp$RelayApplication")
        hookRelayApplicationGetters($$"com.xiaomi.mirror.message.global.proto.RelayAppGlobal$RelayApplication")

        supportAllAppSubScreenMethod?.createHook {
            returnConstant(true)
        }

        // 服务端 App 白名单判断；非白名单 App 的图标、流转和全屏都会经过这里。
        miCloudUtils.createHooks {
            returnConstant(true)
        }
        whitelist?.createHook {
            returnConstant(true)
        }

        // 目标设备能力也会参与判断，false 时图标或副屏入口可能不会出现。
        loadClass("com.xiaomi.mirror.RemoteDeviceInfo").apply {
            findMethod {
                name("isSupportSendApp")
            }.createHook {
                returnConstant(true)
            }

            findMethod {
                name("isSupportSubScreen")
            }.createHook {
                returnConstant(true)
            }
        }

        val relayMessageClass = findClassIfExists("com.xiaomi.mirror.message.RelayAppMessage")

        // RelayAppMessage 是服务端内部流转消息，修正这里可覆盖 copy/fromProto 等二次生成场景。
        relayMessageClass
            ?.declaredMethods
            ?.filter { it.returnType == relayMessageClass }
            ?.createAfterHooks { param ->
                param.result?.unlockRelayAppMessage()
            }

        // Generator 是普通 App 和自定义 App 流转消息的生成入口，防止新消息重新带上限制字段。
        findClassIfExists("com.xiaomi.mirror.message.RelayAppMessageGenerator")
            ?.declaredMethods
            ?.filter { it.returnType == relayMessageClass }
            ?.createAfterHooks { param ->
                param.result?.unlockRelayAppMessage()
            }
    }

    // 只处理和流转限制直接相关的 getter，避免影响 proto 里的其他字段。
    private fun hookRelayApplicationGetters(className: String) {
        val getterResults = mapOf(
            "getIsHideIcon" to false,
            "getSupportHandOff" to true,
            "getSupportSubScreen" to true
        )

        findClassIfExists(className)
            ?.declaredMethods
            ?.filter { it.name in getterResults && it.returnType == Boolean::class.javaPrimitiveType }
            ?.forEach { method ->
                method.createHook {
                    returnConstant(getterResults.getValue(method.name))
                }
            }
    }

    private fun Any.unlockRelayAppMessage() {
        runCatching { setObjectField("isHideIcon", false) }
        runCatching { setObjectField("supportHandoff", true) }
        runCatching { setObjectField("supportSubScreen", true) }
    }
}
