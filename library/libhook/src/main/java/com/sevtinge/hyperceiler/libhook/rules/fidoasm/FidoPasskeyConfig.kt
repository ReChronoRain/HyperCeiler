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

object FidoPasskeyConfig {

    /**
     * web 源组 RP：rpId 命中的请求 origin 改写成 https://<rpId>。
     * 2026-11-24 实测：X、Telegram 创建与登录均接受原生 apk-key-hash 源
     * （base64url 归一后），web 改写已无启用对象——机制保留，名单空置。
     */
    val WEB_RPS = arrayOf<String>()

    /** 其它 App 的策略：保留 apk-key-hash 形式，只修成规范 base64url */
    const val OTHER_APPS_MODE = "base64url"

    /** 伪造的 assetlink 成功响应体（code=0 即 trusted） */
    const val FORGED_ASSETLINK_BODY =
        """{"code":0,"desc":"","clientCacheDurationMills":180000}"""

    /** 闸门模式：off=无条件放行；log=只校验记日志仍放行；enforce=不过就伪造拒绝 */
    const val GATE_MODE_OFF = "off"
    const val GATE_MODE_LOG = "log"
    const val GATE_MODE_ENFORCE = "enforce"

    /** 闸门模式：核验 = 抓 RP 的 assetlinks.json 按 DAL 语义比对，无静态白名单 */
    const val ASSETLINK_GATE_MODE = GATE_MODE_ENFORCE

    /** 短路定位常量串（VendorValidator 抛异常的 format 模板前缀） */
    const val VALIDATOR_NEEDLE = "vendor validate failed"

    /** 辅助定位串（只打日志帮认路） */
    const val AUTH_MANAGER_NEEDLE = "checkAppAuthenticationSync>>>cacheInfo:"

    /** 源级 clientData 改写定位串（ClientDataProcessor 所在类） */
    const val CLIENT_DATA_NEEDLE = "webauthn."

    /** userHandle 保住钩定位串（setUserSelected 所在方法） */
    const val USER_SELECTED_NEEDLE = "setUserSelected isDiscoverCredential:"

    /** 伪造规则表：URL 命中关键字即返回伪造响应体，否则 null */
    fun forgeBodyFor(url: String?): String? {
        if (url == null) return null
        if (url.contains("/pass/verify/assetlink")) return FORGED_ASSETLINK_BODY
        return null
    }

    /** 网络探测日志关注的 URL 关键词判定 */
    fun isInterestingUrl(url: String?): Boolean {
        if (url == null) return false
        val u = url.lowercase()
        return u.contains("account.xiaomi.com")
            || u.contains("/pass/")
            || u.contains("keybox")
            || u.contains("keychain")
            || u.contains("keyapi")
            || u.contains("micloud")
            || u.contains("passkeeper")
            || u.contains("fido")
            || u.contains("assetlink")
            || u.contains("verify")
    }
}
