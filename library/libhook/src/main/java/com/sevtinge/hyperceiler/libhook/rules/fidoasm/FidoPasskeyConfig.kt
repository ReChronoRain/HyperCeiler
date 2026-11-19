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

    val WEB_RPS = arrayOf(
        "telegram.org",
    )

    val WEB_ORIGIN_PACKAGES = arrayOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.messenger.beta",
    )

    const val WEB_ORIGIN = "https://telegram.org"

    const val OTHER_APPS_MODE = "base64url"

    const val FORGED_ASSETLINK_BODY =
        """{"code":0,"desc":"","clientCacheDurationMills":180000}"""

    const val VALIDATOR_NEEDLE = "vendor validate failed"

    const val CLIENT_DATA_NEEDLE = "webauthn."

    fun originModeFor(pkg: String?): String {
        if (pkg != null) {
            for (p in WEB_ORIGIN_PACKAGES) {
                if (p == pkg) return "web"
            }
        }
        return OTHER_APPS_MODE
    }

    fun forgeBodyFor(url: String?): String? {
        if (url == null) return null
        if (url.contains("/pass/verify/assetlink")) return FORGED_ASSETLINK_BODY
        return null
    }

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
