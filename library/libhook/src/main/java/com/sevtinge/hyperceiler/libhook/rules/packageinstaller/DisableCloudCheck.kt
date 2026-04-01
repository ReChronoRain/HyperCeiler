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
package com.sevtinge.hyperceiler.libhook.rules.packageinstaller

import android.content.Context
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.invokeOriginalMethod
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method

object DisableCloudCheck : BaseHook() {

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        method1
        method2
        return true
    }

    private val method1 by lazy<Method> {
        requiredMember("NoNetworkInstallBtn") {
            it.findMethod {
                matcher {
                    usingStrings("install_btn")
                    usingNumbers(16)
                }
            }.single()
        }
    }

    private val method2 by lazy<Method> {
        requiredMember("NormalInstallBtn") {
            it.findMethod {
                matcher {
                    usingStrings("getString(if (shouldUseU…e R.string.start_install)")
                }
            }.single()
        }
    }

    override fun init() {
        // 签名对应：public final java.lang.Object c(Context, g, int, ApkInfo, HashMap, Continuation)
        val methods = DexKit.findMemberList<Method>("DisableCloudCheck_Fetch") {
            it.findMethod {
                matcher {
                    paramCount = 6
                    paramTypes(
                        Context::class.java.name,
                        "",
                        Int::class.javaPrimitiveType?.name ?: "int",
                        "com.miui.packageInstaller.model.ApkInfo",
                        HashMap::class.java.name,
                        "kotlin.coroutines.Continuation"
                    )
                    returnType = Any::class.java.name
                }
            }
        }

        hookMethod(method1, object : IMethodHook {
            override fun before(param: HookParam?) {
                callMethod(param?.thisObject, method2.name, param?.args?.get(0))
                param?.result = null
            }
        })

        methods.forEach { method ->
            try {
                method.createHook {
                    replace { param ->
                        try {
                            val cloudParamsClass = findClass("com.miui.packageInstaller.model.CloudParams")
                            val cloudParamsInstance = cloudParamsClass.newInstance()

                            val successClass = findClass("com.miui.packageInstaller.model.CloudResult\$Success")
                            val successConstructor = successClass.getDeclaredConstructor(cloudParamsClass)

                            successConstructor.newInstance(cloudParamsInstance)
                        } catch (e: Exception) {
                            invokeOriginalMethod(
                                param.executable as Method,
                                param.thisObject,
                                param.args
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
