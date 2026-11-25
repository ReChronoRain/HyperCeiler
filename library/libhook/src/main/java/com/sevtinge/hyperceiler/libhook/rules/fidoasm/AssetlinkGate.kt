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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * 关卡一本地闸门：伪造 code:0 之前的 (rp, packageName, 签名指纹) 核验。
 *
 * 抓 RP 自己的 assetlinks.json 按标准 DAL 语义比对（get_login_creds +
 * namespace=android_app + 包名 + sha256_cert_fingerprints），无静态白名单；
 * 浏览器流比对 web 条目（site=https://<rpId>）。拒绝时伪造忠实失败体
 * （asset_not_support / fingerprints not match），上游抛原生 vendor validate
 * 异常，与真机拒批行为一致。声明每 URL 进程内只抓一次。
 */
object AssetlinkGate {

    /** 裁决：通过 */
    const val PASS = 0
    /** 裁决：(rp, 包名) 不在声明里 → asset_not_support */
    const val DENY_UNLISTED = 1
    /** 裁决：在声明里但签名指纹不符 → fingerprints not match */
    const val DENY_FINGERPRINT = 2

    private const val TAG = "AssetlinkGate"
    private const val REL_LOGIN_CREDS = "delegate_permission/common.get_login_creds"
    private const val DAL_FETCH_TIMEOUT_MS = 5000
    private const val DAL_MAX_BYTES = 65536

    /** 声明抓取缓存：URL → 已解析条目（空表 = 抓到但解析失败） */
    private val declCache: MutableMap<String, JSONArray> = HashMap()

    /** 伪造响应入口：解析表单 → 裁决 → 通过体 / 忠实失败体（log 模式下裁决为拒也给通过体） */
    fun forgeResponse(formBody: String?): String {
        val f = parseForm(formBody)
        val rp = f["rp"]
        val pkg = f["packageName"]
        val fp = f["fingerprints"]
        val isBrowser = f["namespace"] == "web"
        val verdict = check(rp, pkg, fp, f["assetlinkUrlList"], isBrowser)
        return if (verdict == PASS || !isEnforced()) {
            FidoPasskeyConfig.FORGED_ASSETLINK_BODY
        } else {
            denyBody(verdict, pkg)
        }
    }

    /** enforce 模式下拒绝才真正执行；off/log 模式一律放行 */
    fun isEnforced(): Boolean {
        return FidoPasskeyConfig.GATE_MODE_ENFORCE == FidoPasskeyConfig.ASSETLINK_GATE_MODE
    }

    /** 裁决名（日志用） */
    fun verdictName(verdict: Int): String {
        if (verdict == DENY_FINGERPRINT) return "FINGERPRINT-MISMATCH"
        if (verdict == DENY_UNLISTED) return "NOT-IN-DECLARATION"
        return "PASS"
    }

    /**
     * 闸门裁决，返回真实裁决（PASS / DENY_*），模式不折进返回值：
     * off 不评估直接 PASS；log 只记日志，放行与否由调用方按 [isEnforced] 决定；
     * enforce 时调用方据裁决走失败路径。
     */
    fun check(
        rp: String?, pkg: String?, fp: String?,
        assetlinkUrlListJson: String?, isBrowser: Boolean
    ): Int {
        if (FidoPasskeyConfig.GATE_MODE_OFF == FidoPasskeyConfig.ASSETLINK_GATE_MODE) return PASS
        val verdict = evaluate(rp, pkg, fp, assetlinkUrlListJson, isBrowser)
        if (verdict == PASS) {
            XposedLog.i(TAG, pkg, "GATE PASS rp=$rp pkg=$pkg")
        } else {
            XposedLog.i(
                TAG, pkg, "GATE ${verdictName(verdict)} rp=$rp pkg=$pkg fp=$fp" +
                    (if (isEnforced()) " -> DENY" else " -> 放行(log 模式)")
            )
        }
        return verdict
    }

    private fun evaluate(
        rp: String?, pkg: String?, fp: String?,
        assetlinkUrlListJson: String?, isBrowser: Boolean
    ): Int {
        if (rp.isNullOrEmpty()) return DENY_UNLISTED
        if (isBrowser) return webDalCheck(rp, assetlinkUrlListJson)
        if (pkg.isNullOrEmpty()) return DENY_UNLISTED
        return dalCheck(rp, pkg, fp, assetlinkUrlListJson)
    }

    /** 浏览器流：声明里找 namespace=web + site=https://<rpId> + get_login_creds 条目 */
    private fun webDalCheck(rp: String, assetlinkUrlListJson: String?): Int {
        for (url in declarationUrls(rp, assetlinkUrlListJson)) {
            val arr = declarations(url)
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                if (!arrayContains(entry.optJSONArray("relation"), REL_LOGIN_CREDS)) continue
                val target = entry.optJSONObject("target") ?: continue
                if (target.optString("namespace") != "web") continue
                if (siteMatches(target.optString("site", null), rp)) return PASS
            }
        }
        return DENY_UNLISTED
    }

    private fun siteMatches(declaredSite: String?, rp: String): Boolean {
        if (declaredSite == null) return false
        var s = declaredSite.trim()
        while (s.endsWith("/")) s = s.substring(0, s.length - 1)
        return s.equals("https://$rp", ignoreCase = true)
    }

    /** 抓 assetlinks.json 做标准 DAL 比对（每 URL 进程内只抓一次） */
    private fun dalCheck(rp: String, pkg: String, fp: String?, assetlinkUrlListJson: String?): Int {
        var verdict = DENY_UNLISTED
        var pkgSeen = false
        for (url in declarationUrls(rp, assetlinkUrlListJson)) {
            val arr = declarations(url)
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                if (!arrayContains(entry.optJSONArray("relation"), REL_LOGIN_CREDS)) continue
                val target = entry.optJSONObject("target") ?: continue
                if (target.optString("namespace") != "android_app") continue
                if (pkg != target.optString("package_name")) continue
                pkgSeen = true
                val fps = target.optJSONArray("sha256_cert_fingerprints")
                if (fps != null) {
                    for (k in 0 until fps.length()) {
                        if (fpEquals(fps.optString(k, null), fp)) {
                            verdict = PASS
                            break
                        }
                    }
                }
                if (verdict == PASS) break
            }
            if (verdict == PASS) break
        }
        if (verdict != PASS && pkgSeen) verdict = DENY_FINGERPRINT
        return verdict
    }

    /**
     * 声明抓取缓存：每个 URL 进程内只抓一次（同一次凭据操作闸门判两遍不重复联网）。
     * 解析失败缓存空表（告警只报一次）；网络失败不缓存、下次重试。
     */
    private fun declarations(url: String): JSONArray {
        synchronized(declCache) {
            val cached = declCache[url]
            if (cached != null) return cached
        }
        var arr = JSONArray()
        val doc = httpGet(url)
        if (doc == null) return arr
        try {
            arr = JSONArray(doc)
        } catch (t: Throwable) {
            XposedLog.w(TAG, "GATE dal parse err $url: $t")
        }
        synchronized(declCache) {
            declCache[url] = arr
        }
        return arr
    }

    /**
     * 声明地址：<rp>/.well-known/assetlinks.json 恒定核验；请求体里的
     * assetlinkUrlList 仅采信 rp 域名内（含子域）的 URL——防调用方塞私有 URL 伪造声明。
     */
    private fun declarationUrls(rp: String?, assetlinkUrlListJson: String?): Array<String> {
        val out = ArrayList<String>()
        if (rp != null && rp.matches(Regex("[a-zA-Z0-9.-]+"))) {
            out.add("https://$rp/.well-known/assetlinks.json")
        }
        if (!assetlinkUrlListJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(assetlinkUrlListJson)
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i, null)
                    if (u != null && u.startsWith("https://") && inRpDomain(u, rp) && !out.contains(u)) {
                        out.add(u)
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return out.toTypedArray()
    }

    private fun inRpDomain(url: String, rp: String?): Boolean {
        if (rp.isNullOrEmpty()) return false
        return try {
            val host = URL(url).host
            host != null && (host == rp || host.endsWith(".$rp"))
        } catch (t: Throwable) {
            false
        }
    }

    private fun httpGet(url: String): String? {
        var c: HttpURLConnection? = null
        return try {
            c = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = DAL_FETCH_TIMEOUT_MS
                readTimeout = DAL_FETCH_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json")
            }
            if (c.responseCode != 200) return null
            val input = c.inputStream
            val bos = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var total = 0
            while (total < DAL_MAX_BYTES) {
                val n = input.read(buf)
                if (n <= 0) break
                bos.write(buf, 0, n)
                total += n
            }
            runCatching { input.close() }
            String(bos.toByteArray(), StandardCharsets.UTF_8)
        } catch (t: Throwable) {
            XposedLog.w(TAG, "GATE dal fetch err $url: $t")
            null
        } finally {
            c?.disconnect()
        }
    }

    /** 忠实失败体：code/desc 语义照抄真机抓包（"fount" 为服务端原文 typo，故意保留） */
    fun denyBody(verdict: Int, pkg: String?): String {
        return try {
            val o = JSONObject()
            o.put("code", 210003)
            if (verdict == DENY_FINGERPRINT) {
                o.put("desc", "fingerprints not match")
                o.put("msg", "fingerprints not match")
            } else {
                o.put("desc", "asset_not_support")
                o.put("msg", "cannot be fount packagename:" + (pkg ?: ""))
            }
            o.toString()
        } catch (t: Throwable) {
            "{\"code\":210003,\"desc\":\"asset_not_support\"}"
        }
    }

    /** 表单解析：k=v&k=v 拆分，值原样取用（拼表单时已解码、未编码上 wire） */
    private fun parseForm(body: String?): Map<String, String> {
        val out = HashMap<String, String>()
        if (body == null) return out
        for (part in body.split("&")) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            out[part.substring(0, eq)] = part.substring(eq + 1)
        }
        return out
    }

    /** 指纹比对：忽略大小写与冒号 */
    private fun fpEquals(a: String?, b: String?): Boolean {
        val na = normalizeFp(a)
        val nb = normalizeFp(b)
        return na.isNotEmpty() && na == nb
    }

    private fun normalizeFp(fp: String?): String {
        if (fp == null) return ""
        return fp.replace(":", "").replace(" ", "").uppercase(Locale.ROOT)
    }

    private fun arrayContains(arr: JSONArray?, value: String): Boolean {
        if (arr == null) return false
        for (i in 0 until arr.length()) {
            if (value == arr.optString(i, null)) return true
        }
        return false
    }
}
