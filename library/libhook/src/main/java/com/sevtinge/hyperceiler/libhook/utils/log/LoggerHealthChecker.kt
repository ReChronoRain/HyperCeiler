/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.log

import android.util.Log
import com.sevtinge.hyperceiler.libhook.utils.log.LoggerHealthChecker.ALIVE_THRESHOLD
import com.sevtinge.hyperceiler.libhook.utils.log.LoggerHealthChecker.confidence
import com.sevtinge.hyperceiler.libhook.utils.log.LoggerHealthChecker.diagSummary
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.rootExecCmd
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 日志服务健康检查器
 *
 * 置信度 >= [ALIVE_THRESHOLD] 视为日志服务可用。
 */
object LoggerHealthChecker {

    @JvmField
    @Volatile
    var confidence: Int = 0

    @JvmField
    @Volatile
    var diagSummary: String = "NOT_CHECKED"

    private const val TAG = "HyperCeilerLogManager"

    const val ALIVE_THRESHOLD = 60

    // ============ 各项权重 ============
    private const val WEIGHT_LOGCAT = 30
    private const val WEIGHT_XPOSED_MODULES = 40
    private const val WEIGHT_XPOSED_VERBOSE = 30

    private const val LOGCAT_READ_DELAY_MS = 150L
    private const val MAX_RETRIES = 3

    /**
     * 执行所有检查，更新 [confidence] 和 [diagSummary]。
     * @return confidence >= [ALIVE_THRESHOLD]
     */
    @JvmStatic
    fun isLoggerAlive(): Boolean {
        var score = 0
        val diag = mutableListOf<String>()

        // Logcat 管道
        val logcatResult = checkLogcat()
        score += logcatResult.score
        diag.add("logcat=${logcatResult.tag}")

        // Xposed modules 日志
        val modulesResult = checkXposedLogFile("modules")
        score += modulesResult.score
        diag.add("xp_mod=${modulesResult.tag}")

        // Xposed verbose 日志
        val verboseResult = checkXposedLogFile("verbose")
        score += verboseResult.score
        diag.add("xp_verb=${verboseResult.tag}")

        confidence = score.coerceIn(0, 100)
        diagSummary = diag.joinToString(" ")

        return confidence >= ALIVE_THRESHOLD
    }

    /**
     * 状态等级文案
     */
    private fun statusLevel(): String = when {
        confidence >= 80 -> "HEALTHY"
        confidence >= ALIVE_THRESHOLD -> "DEGRADED"
        confidence > 0 -> "UNHEALTHY"
        else -> "DEAD"
    }

    /**
     * 简短状态（用于弹窗 UI）
     * 示例："HEALTHY(100%)" / "DEAD(0%)"
     */
    @JvmStatic
    fun formatStatus(): String = "${statusLevel()}($confidence%)"

    /**
     * 详细状态（用于设备信息/调试日志）
     * 示例："HEALTHY(100%) logcat=OK xp_mod=OK xp_verb=OK"
     */
    @JvmStatic
    fun formatDetailedStatus(): String {
        return "${statusLevel()}($confidence%) $diagSummary"
    }

    // ==================== 检查项 ====================

    private data class CheckResult(val score: Int, val tag: String)

    /**
     * Logcat 管道读写检查（不依赖 root）
     */
    private fun checkLogcat(): CheckResult {
        val token = "ALIVE_${android.os.Process.myPid()}_${System.nanoTime()}"
        Log.d(TAG, token)

        repeat(MAX_RETRIES) { attempt ->
            try {
                Thread.sleep(LOGCAT_READ_DELAY_MS * (attempt + 1))
                if (readLogcatForToken(token)) {
                    return CheckResult(WEIGHT_LOGCAT, "OK")
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return CheckResult(0, "INTERRUPTED")
            } catch (e: Exception) {
                return CheckResult(0, "ERROR:${e.message?.take(50)}")
            }
        }
        return CheckResult(0, "NOT_FOUND")
    }

    private fun readLogcatForToken(token: String): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-v", "brief", "-s", "$TAG:D")
            )
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                generateSequence { reader.readLine() }.any { it.contains(token) }
            }
        } finally {
            process?.destroy()
        }
    }

    /**
     * 检查 Xposed 日志文件（需要 root）
     * @param prefix "modules" 或 "verbose"
     */
    private fun checkXposedLogFile(prefix: String): CheckResult {
        val weight = if (prefix == "modules") WEIGHT_XPOSED_MODULES else WEIGHT_XPOSED_VERBOSE
        return try {
            val dirExists =
                rootExecCmd("ls -d /data/adb/lspd/log/ 2>/dev/null")?.isNotEmpty() == true
            if (!dirExists) return CheckResult(0, "NO_DIR")

            val latestFile =
                rootExecCmd("ls -t /data/adb/lspd/log/${prefix}_*.log 2>/dev/null | head -n 1")
                    ?.trim() ?: ""
            if (latestFile.isEmpty() || latestFile.contains("No such file")) {
                return CheckResult(0, "NO_FILE")
            }

            val grepOutput = rootExecCmd(
                "grep -i -q 'HyperCeiler' $latestFile && echo 'FOUND' || echo 'EMPTY'"
            )
            if (grepOutput?.trim() == "FOUND") {
                CheckResult(weight, "OK")
            } else {
                // 文件存在但无 HyperCeiler 记录 → 给四分之一分
                CheckResult(weight / 4, "EMPTY")
            }
        } catch (e: Exception) {
            // 获取失败（如权限问题） → 给一半分并记录错误
            CheckResult(weight / 2, "ERROR:${e.message?.take(50)}")
        }
    }

    // ==================== 修复工具 ====================

    @JvmStatic
    fun fixLSPosedLogService(): String {
        return try {
            rootExecCmd("resetprop -n persist.log.tag.LSPosed V")
            rootExecCmd("resetprop -n persist.log.tag.LSPosed-Bridge V")
            "SUCCESS"
        } catch (e: Exception) {
            e.toString()
        }
    }
}
