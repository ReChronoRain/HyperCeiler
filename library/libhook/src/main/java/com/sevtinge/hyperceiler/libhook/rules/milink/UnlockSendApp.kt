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
package com.sevtinge.hyperceiler.libhook.rules.milink

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockSendApp : BaseHook() {

    private val subScreenApps by lazy<List<Method>> {
        optionalMemberList("subScreenApps") { bridge ->
            bridge.findClass {
                matcher {
                    addUsingString("ML::SubScreenApps", StringMatchType.Equals)
                }
            }.findMethod {
                matcher {
                    returnType = "boolean"
                }
            }
        }
    }

    private val appCirculateClientHelper by lazy {
        optionalMember("appCirculateClientHelper") { bridge ->
            bridge.findClass {
                matcher {
                    addUsingString("AppCirculateClientHelper")
                }
            }.findMethod {
                matcher {
                    addInvoke("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;")
                    modifiers = Modifier.PUBLIC
                    returnType = "boolean"
                }
            }.single()
        } as? Method
    }

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        subScreenApps
        appCirculateClientHelper
        return true
    }

    override fun init() {
        // 设备互联入口会先询问 SDK 当前 App 是否允许流转到平板/PC/手机。
        loadClass("com.xiaomi.mirror.synergy.MiuiSynergySdk").apply {
            findMethod {
                name("isSupportSendApp")
            }.createHook {
                returnConstant(true)
            }

            findMethod {
                name("isSupportSendAppToPhone")
            }.createHook {
                returnConstant(true)
            }
        }

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

        // 本地副屏管理使用这个白名单判断当前 App 是否允许全屏或拖拽到目标屏幕。
        subScreenApps.createHooks {
            returnConstant(true)
        }

        // 设备列表 UI 会在最终展示前再判断一次当前 App 对目标设备是否可流转。
        appCirculateClientHelper?.createHook {
            returnConstant(true)
        }
    }
}
