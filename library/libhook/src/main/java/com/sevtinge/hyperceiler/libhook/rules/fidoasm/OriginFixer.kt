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

import android.util.Base64
import com.sevtinge.hyperceiler.common.log.XposedLog
import org.json.JSONObject
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.Collections
import java.util.Deque
import java.util.IdentityHashMap

object OriginFixer {

    private const val TAG = "OriginFixer"

    fun isWebRp(rpId: String?): Boolean {
        if (rpId == null) return false
        for (r in FidoPasskeyConfig.WEB_RPS) {
            if (r == rpId) return true
        }
        return false
    }

    fun rewriteOrigin(origin: String?, rpId: String?): String? {
        if (origin.isNullOrEmpty()) return null

        if (isWebRp(rpId)) {
            return if (origin.startsWith("https://")) null else "https://$rpId"
        }
        if (FidoPasskeyConfig.OTHER_APPS_MODE == "base64url"
            && origin.startsWith("android:apk-key-hash:")
        ) {
            // apk-key-hash 要求无 padding 的 base64url；客户端给出的编码不规范时会被
            // 严格校验拒，这里解码后重编码归一（幂等：已规范则返回 null 不动）
            val hash = origin.substring("android:apk-key-hash:".length)
            val out = "android:apk-key-hash:" + b64encodeUrlSafe(b64decode(hash))
            return if (out == origin) null else out
        }
        return null
    }

    fun findWebRpInObject(root: Any?, maxDepth: Int): String? {
        if (root == null) return null
        val queue: Deque<Array<Any>> = ArrayDeque()
        val visited: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())
        queue.add(arrayOf(root, 0))
        while (queue.isNotEmpty()) {
            val item = queue.poll()
            val o = item[0]
            val depth = item[1] as Int
            if (o == null || !visited.add(o)) continue
            if (o is String) {
                if (isWebRp(o)) return o
                continue
            }
            val cls = o.javaClass
            if (cls.isPrimitive || cls.isEnum || cls.isArray
                || cls.classLoader == null
            ) {
                continue
            }
            if (depth >= maxDepth) continue
            for (f in cls.declaredFields) {
                if (Modifier.isStatic(f.modifiers)) continue
                try {
                    f.isAccessible = true
                    val v = f.get(o)
                    if (v is String) {
                        if (isWebRp(v)) return v
                    } else if (v != null && depth < maxDepth) {
                        queue.add(arrayOf(v, depth + 1))
                    }
                } catch (_: Throwable) {
                }
            }
        }
        return null
    }

    fun fixClientData(cjd: String, rpId: String?): String? {
        return try {
            val inner = JSONObject(cjd)
            val pkg = inner.optString("androidPackageName", "")
            val oldOrigin = inner.optString("origin", "")
            val newOrigin = rewriteOrigin(oldOrigin, rpId)
            if (newOrigin == null || newOrigin == oldOrigin) return null
            inner.put("origin", newOrigin)
            XposedLog.i(
                TAG, "ORIGIN FIX(src)[$pkg,rp=$rpId]: $oldOrigin" +
                    "\n                -> $newOrigin"
            )
            inner.toString()
        } catch (t: Throwable) {
            XposedLog.w(TAG, "fixClientData err", t)
            null
        }
    }

    fun b64decode(s: String): ByteArray {
        val t = s.replace('-', '+').replace('_', '/')
        return Base64.decode(t, Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun b64encodeUrlSafe(b: ByteArray): String {
        return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
