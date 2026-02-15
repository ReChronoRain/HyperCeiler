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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIntField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setIntField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object UnlockFoucsAuth : BaseHook() {

    override fun init() {
        val authSessionClass = loadClass("com.xiaomi.xms.auth.AuthSession")

        authSessionClass.methodFinder()
            .filterByName("b")
            .filterByParamCount(1)
            .first()
            .createHook {
                before {
                    val error = it.args[0]
                    if (error != null) {
                        val errorCode = error.getIntField("a") // 假设 a 是 errorCode 字段
                        XposedLog.w(
                            TAG,
                            lpparam.packageName,
                            "[XMS][Auth] 发现错误分发: $errorCode，正在拦截并强制返回成功"
                        )
                        error.setIntField("a", 0)

                        it.result = it.thisObject.callMethod("h")
                    }
                }
            }
    }
}
