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
package com.sevtinge.hyperceiler.libhook.rules.fidoasm

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object BypassVendorValidate : BaseHook() {

    private lateinit var validatorClasses: List<Class<*>>

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        validatorClasses = requiredMemberList("vendorValidator") { bridge ->
            bridge.findClass {
                matcher {
                    addUsingString(FidoPasskeyConfig.VALIDATOR_NEEDLE)
                }
            }
        }
        return true
    }

    override fun init() {
        var hooked = 0
        for (cls in validatorClasses) {
            for (m in cls.declaredMethods) {
                val p = m.parameterTypes
                if (m.returnType == Void.TYPE
                    && p.size == 5
                    && p[0] == String::class.java
                    && p[1] == String::class.java
                    && p[2] == String::class.java
                    && p[3] == java.lang.Boolean.TYPE
                ) {
                    m.createBeforeHook {
                        it.result = null
                        XposedLog.i(TAG, packageName, "SHORT-CIRCUIT vendor validate")
                    }
                    XposedLog.i(
                        TAG, packageName, "vendor validate bypass hook: " +
                            "${cls.name}->${m.name}(String,String,String,boolean,${p[4].name})"
                    )
                    hooked++
                }
            }
        }
        if (hooked == 0) {
            XposedLog.w(TAG, packageName, "SHORT-CIRCUIT: no method matched（目标 APK 可能升级改了结构）")
        }
    }
}
