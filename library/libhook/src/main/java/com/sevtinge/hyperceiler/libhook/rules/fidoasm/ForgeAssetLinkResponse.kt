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
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.reflect.Method
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.Collections

object ForgeAssetLinkResponse : BaseHook() {

    private const val MAX_LOG_BODY = 600

    private val hookedConnClasses: MutableSet<Class<*>> =
        Collections.synchronizedSet(HashSet())
    private val hookedMethods: MutableSet<Method> =
        Collections.synchronizedSet(HashSet())

    override fun init() {
        for (method in URL::class.java.declaredMethods) {
            if (method.name != "openConnection") continue
            method.createAfterHook { param ->
                val conn = param.result
                if (conn is URLConnection) hookConnClassOnce(conn.javaClass)
            }
        }
        XposedLog.i(
            TAG, packageName,
            "NetHook installed (netlog=${PrefsBridge.getBoolean("development_debug_mode")})"
        )
    }

    private fun hookConnClassOnce(cls: Class<*>) {
        if (!hookedConnClasses.add(cls)) return
        val isHttp = HttpURLConnection::class.java.isAssignableFrom(cls)

        hookNoArgOnce(cls, "connect") { method ->
            method.createBeforeHook { param ->
                if (forgedUrl(param.thisObjectOrNull) != null) param.result = null
            }
        }

        if (isHttp) {
            hookNoArgOnce(cls, "getResponseCode") { method ->
                method.createBeforeHook { param ->
                    if (forgedUrl(param.thisObjectOrNull) != null) param.result = 200
                }
            }
        }

        hookNoArgOnce(cls, "getOutputStream") { method ->
            method.createBeforeHook { param ->
                if (forgedUrl(param.thisObjectOrNull) != null) param.result = ByteArrayOutputStream()
            }
        }

        hookNoArgOnce(cls, "getHeaderFields") { method ->
            method.createBeforeHook { param ->
                if (forgedUrl(param.thisObjectOrNull) != null) param.result = HashMap<String, List<String>>()
            }
        }

        hookNoArgOnce(cls, "getInputStream") { method ->
            method.createHook {
                before { param ->
                    val conn = param.thisObjectOrNull
                    if (forgedUrl(conn) != null) {
                        XposedLog.i(TAG, packageName, "FORGE response <= ${describe(conn)}")
                        param.result = ByteArrayInputStream(
                            FidoPasskeyConfig.FORGED_ASSETLINK_BODY.toByteArray(Charsets.UTF_8)
                        )
                    }
                }
                after { param ->
                    if (!PrefsBridge.getBoolean("development_debug_mode")) return@after
                    val conn = param.thisObjectOrNull ?: return@after
                    if (forgedUrl(conn) != null) return@after
                    val r = param.result
                    if (r !is InputStream) return@after
                    if (!FidoPasskeyConfig.isInterestingUrl(safeUrl(conn))) return@after
                    try {
                        val bos = ByteArrayOutputStream()
                        val buf = ByteArray(4096)
                        var total = 0
                        while (total < 65536) {
                            val n = r.read(buf)
                            if (n <= 0) break
                            bos.write(buf, 0, n)
                            total += n
                        }
                        runCatching { r.close() }
                        val body = bos.toByteArray()
                        XposedLog.i(
                            TAG, packageName,
                            "NET <= ${describe(conn)} body(${body.size}B)=${preview(body)}"
                        )
                        param.result = ByteArrayInputStream(body)
                    } catch (t: Throwable) {
                        XposedLog.w(TAG, packageName, "netlog read err", t)
                    }
                }
            }
        }

        XposedLog.i(TAG, packageName, "conn class hooked: ${cls.name}")
    }

    private fun hookNoArgOnce(cls: Class<*>, name: String, install: (Method) -> Unit) {
        val method = findNoArg(cls, name) ?: return
        if (!hookedMethods.add(method)) return
        install(method)
    }

    private fun findNoArg(cls: Class<*>, name: String): Method? {
        var c: Class<*>? = cls
        while (c != null) {
            for (m in c.declaredMethods) {
                if (m.name == name && m.parameterCount == 0) return m
            }
            c = c.superclass
        }
        return null
    }

    private fun forgedUrl(conn: Any?): String? {
        return FidoPasskeyConfig.forgeBodyFor(safeUrl(conn))
    }

    private fun safeUrl(conn: Any?): String? {
        return try {
            (conn as? URLConnection)?.url?.toString()
        } catch (t: Throwable) {
            null
        }
    }

    private fun describe(conn: Any?): String {
        val method = runCatching { callMethod(conn, "getRequestMethod") }.getOrNull() ?: "?"
        return "$method ${safeUrl(conn)}"
    }

    private fun preview(body: ByteArray): String {
        val s = String(body, 0, minOf(body.size, MAX_LOG_BODY), Charsets.UTF_8)
        return s.replace('\n', ' ').replace('\r', ' ')
    }
}
