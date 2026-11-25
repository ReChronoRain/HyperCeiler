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
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.security.MessageDigest

object FixClientDataOrigin : BaseHook() {

    private val rpContext = ThreadLocal<String>()

    private lateinit var processorClasses: List<Class<*>>

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        // optionalMemberList：空命中不抛异常，由 init() 的 NOTHING hooked 警告按源码语义提示重定位
        processorClasses = optionalMemberList("clientDataProcessor") { bridge ->
            bridge.findClass {
                matcher {
                    addUsingString(FidoPasskeyConfig.CLIENT_DATA_NEEDLE)
                }
            }
        }
        return true
    }

    override fun init() {
        var hooked = 0
        for (cls in processorClasses) {
            for (m in cls.declaredMethods) {
                val ret = m.returnType
                val p = m.parameterTypes

                if (p.size <= 1 && ret != Void.TYPE && !ret.isPrimitive
                    && !ret.isArray && hasOnlyByteArrayFields(ret)
                ) {
                    m.createHook {
                        before { param ->
                            rpContext.set(
                                OriginFixer.findWebRpInObject(
                                    param.args.getOrNull(0), 3
                                )
                            )
                        }
                        after { param ->
                            try {
                                fixCarrier(param.result)
                            } finally {
                                rpContext.remove()
                            }
                        }
                    }
                    XposedLog.i(TAG, packageName, "clientData hook: ${cls.name}->${m.name} (carrier)")
                    hooked++
                }

                if (ret == ByteArray::class.java && p.size == 1 && p[0] == ByteArray::class.java) {
                    m.createBeforeHook { param ->
                        val bytes = param.args[0] as? ByteArray
                        if (bytes != null) {
                            val fixed = OriginFixer.fixClientData(
                                String(bytes, Charsets.UTF_8), rpContext.get()
                            )
                            if (fixed != null) {
                                param.args[0] = fixed.toByteArray(Charsets.UTF_8)
                            }
                        }
                    }
                    XposedLog.i(TAG, packageName, "clientData hook: ${cls.name}->${m.name} (hash-input)")
                    hooked++
                }
            }
        }
        if (hooked == 0) {
            XposedLog.w(TAG, packageName, "clientData: NOTHING hooked —— 结构变了，重新定位")
        }
    }

    /** carrier 形态判别：字段全为 byte[] 的纯载体类（clientDataJSON + hash），按形态认出口、不依赖类名 */
    private fun hasOnlyByteArrayFields(cls: Class<*>): Boolean {
        val fs = cls.declaredFields
        if (fs.isEmpty()) return false
        for (f in fs) {
            if (f.type != ByteArray::class.java) return false
        }
        return true
    }

    private fun fixCarrier(carrier: Any?) {
        try {
            if (carrier == null) return
            var jsonField: java.lang.reflect.Field? = null
            var hashField: java.lang.reflect.Field? = null
            var json: ByteArray? = null
            // 按内容认字段：含 "\"origin\"" 的是 clientDataJSON，32 字节的是 SHA-256 hash
            for (f in carrier.javaClass.declaredFields) {
                f.isAccessible = true
                val v = f.get(carrier) as? ByteArray ?: continue
                val s = String(v, Charsets.UTF_8)
                if (s.contains("\"origin\"")) {
                    json = v
                    jsonField = f
                } else if (v.size == 32) {
                    hashField = f
                }
            }
            val jsonBytes = json ?: return
            val fixed = OriginFixer.fixClientData(
                String(jsonBytes, Charsets.UTF_8), rpContext.get()
            ) ?: return
            val fixedBytes = fixed.toByteArray(Charsets.UTF_8)
            jsonField?.set(carrier, fixedBytes)
            hashField?.set(carrier, MessageDigest.getInstance("SHA-256").digest(fixedBytes))
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "fixCarrier err", t)
        }
    }
}
