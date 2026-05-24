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
package com.sevtinge.hyperceiler.libhook.rules.xmsf

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockFoucsAuth : BaseHook() {
    private lateinit var getAuthError: Method
    private lateinit var getAuthSuccess: Method
    private lateinit var getErrorField: Field

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        getAuthError = requiredMember("getAuthError") { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    className = "com.xiaomi.xms.auth.AuthSession"
                }
            }.findMethod {
                matcher {
                    modifiers = Modifier.FINAL
                    paramCount = 1
                    returnType = "android.os.Bundle"
                }
            }.single()
        }
        getAuthSuccess = requiredMember("getAuthSuccess") { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    className = "com.xiaomi.xms.auth.AuthSession"
                }
            }.findMethod {
                matcher {
                    modifiers = Modifier.FINAL
                    paramCount = 0
                    returnType = "android.os.Bundle"
                }
            }.single()
        }
        getErrorField = requiredMember("getErrorField") { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    className = "com.xiaomi.xms.auth.AuthError"
                }
            }.findField {
                matcher {
                    type = "int"
                }
            }.single()
        }
        return true
    }

    override fun init() {
        getAuthError.createBeforeHook {
            val error = it.args[0] ?: return@createBeforeHook
            val errorCode = getErrorField.get(error)
            XposedLog.d(TAG, lpparam.packageName, "发现错误分发: $errorCode，正在拦截并强制返回成功")
            getErrorField.set(error, 0)
            it.result = it.thisObject.callMethod(getAuthSuccess.name)
        }

    }
}
