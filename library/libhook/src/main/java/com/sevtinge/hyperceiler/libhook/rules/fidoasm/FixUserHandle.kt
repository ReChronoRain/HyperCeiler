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
import java.lang.reflect.Method

/**
 * 保住登录断言的 userHandle（MiFido 非 discoverable（allowList）断言自抹 user）。
 *
 * setUserSelected 按 CTAP2 语义把 user 抹掉后重新序列化，但 WebAuthn 响应的
 * userHandle 正是 user.id，X/Telegram 登录要求它——表现为创建成功、登录报错。
 * userHandle 不在 TEE 签名覆盖内（签名只盖 authData ‖ clientDataHash），补回安全。
 * 修法：isDiscoverCredential 翻 true，走保留 user + userSelected=true 分支。
 */
object FixUserHandle : BaseHook() {

    private lateinit var targets: List<Method>

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        // optionalMemberList：空命中不抛异常，由 init() 的 NOT hooked 警告按源码语义提示重定位
        targets = optionalMemberList("userSelected") { bridge ->
            bridge.findMethod {
                matcher {
                    addUsingString(FidoPasskeyConfig.USER_SELECTED_NEEDLE)
                }
            }
        }
        return true
    }

    override fun init() {
        var hooked = 0
        for (m in targets) {
            val p = m.parameterTypes
            if (m.returnType == ByteArray::class.java && p.size == 2
                && p[0] == ByteArray::class.java && p[1] == java.lang.Boolean.TYPE
            ) {
                m.createBeforeHook { param ->
                    try {
                        if (java.lang.Boolean.FALSE == param.args.getOrNull(1)) {
                            param.args[1] = true
                            XposedLog.i(TAG, packageName, "userHandle fix: isDiscoverCredential false->true（保住 user/userHandle）")
                        }
                    } catch (t: Throwable) {
                        XposedLog.w(TAG, packageName, "userHandle fix err", t)
                    }
                }
                XposedLog.i(TAG, packageName, "userHandle fix hook: ${m.declaringClass.name}->${m.name}([BZ)[B")
                hooked++
            }
        }
        if (hooked == 0) {
            XposedLog.w(TAG, packageName, "userHandle fix: NOT hooked —— 目标 APK 结构变了，重新定位")
        }
    }
}
