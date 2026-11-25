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
        // 辅助定位串，只打日志帮认路（目标 APK 变更时据此重定位）
        val probe: List<Class<*>> = optionalMemberList("authManagerProbe") { bridge ->
            bridge.findClass {
                matcher {
                    addUsingString(FidoPasskeyConfig.AUTH_MANAGER_NEEDLE)
                }
            }
        }
        XposedLog.i(TAG, packageName, "locator[auth-manager] hits: ${probe.map { it.name }}")
        // optionalMemberList：空命中不抛异常，由 init() 的 hooked==0 警告按源码语义提示重定位
        validatorClasses = optionalMemberList("vendorValidator") { bridge ->
            bridge.findClass {
                matcher {
                    addUsingString(FidoPasskeyConfig.VALIDATOR_NEEDLE)
                }
            }
        }
        XposedLog.i(TAG, packageName, "locator[vendor-validator] hits: ${validatorClasses.map { it.name }}")
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
                        // 入参 = (rpId, packageName, certSha256, isBrowser, listener)
                        val rp = it.args[0] as? String
                        val pkg = it.args[1] as? String
                        val fp = it.args[2] as? String
                        if (rp == null || pkg == null) {
                            // 形态漂移认不出入参：按旧行为放行并报警
                            XposedLog.w(TAG, packageName, "GATE: validate args unreadable（目标 APK 形态漂移?）按旧行为放行")
                            it.result = null
                        } else {
                            val isBrowser = it.args[3] == true
                            val verdict = AssetlinkGate.check(rp, pkg, fp, null, isBrowser)
                            if (verdict == AssetlinkGate.PASS || !AssetlinkGate.isEnforced()) {
                                // 过闸才短路；[gate X] 是真实裁决，非 PASS 时是 log 模式放行
                                it.result = null
                                XposedLog.i(
                                    TAG, packageName,
                                    "SHORT-CIRCUIT vendor validate [gate ${AssetlinkGate.verdictName(verdict)}" +
                                        (if (verdict == AssetlinkGate.PASS) "" else "->log放行") + "] rp=$rp pkg=$pkg"
                                )
                            } else {
                                // 不短路：原方法 POST → NetForge 闸门伪造拒绝 → 原生失败异常
                                XposedLog.i(TAG, packageName, "GATE DENY -> 走原生失败路径 rp=$rp pkg=$pkg fp=$fp")
                            }
                        }
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
