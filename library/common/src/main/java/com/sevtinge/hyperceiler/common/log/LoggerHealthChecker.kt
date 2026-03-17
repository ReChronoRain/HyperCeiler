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
package com.sevtinge.hyperceiler.common.log

import android.os.Process.myPid
import android.util.Log
import com.sevtinge.hyperceiler.common.log.LoggerHealthChecker.ALIVE_THRESHOLD
import com.sevtinge.hyperceiler.common.log.LoggerHealthChecker.confidence
import com.sevtinge.hyperceiler.common.log.LoggerHealthChecker.diagSummary
import com.sevtinge.hyperceiler.common.utils.ShellUtils.rootExecCmd
import java.io.BufferedReader
import java.io.File
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

    @JvmField
    @Volatile
    var localLogBaseDir: File? = null

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
        val token = "ALIVE_${myPid()}_${System.nanoTime()}"
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
        return CheckResult(WEIGHT_LOGCAT / 2, "NOT_FOUND")
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
     * 检查 XposedLogLoader 同步到本地的 Xposed 日志文件（不依赖 root）
     * @param prefix "modules" 或 "verbose"
     */
    private fun checkXposedLogFile(prefix: String): CheckResult {
        val weight = if (prefix == "modules") WEIGHT_XPOSED_MODULES else WEIGHT_XPOSED_VERBOSE
        val logBase = localLogBaseDir ?: return CheckResult(0, "NO_DIR")

        val lspdLogDir = File(logBase, "lspd/log")
        val lspdLogOldDir = File(logBase, "lspd/log.old")

        val latestFile = findLatestLogFile(lspdLogDir, prefix)
            ?: findLatestLogFile(lspdLogOldDir, prefix)
            ?: return CheckResult(0, "NO_FILE")

        return try {
            if (latestFile.length() == 0L) {
                return CheckResult(weight / 4, "EMPTY_FILE")
            }
            val found = latestFile.bufferedReader().use { reader ->
                reader.lineSequence().any { it.contains("HyperCeiler") }
            }
            if (found) CheckResult(weight, "OK")
            else CheckResult(weight / 4, "EMPTY")
        } catch (e: Exception) {
            CheckResult(0, "READ_ERR:${e.message?.take(40)}")
        }
    }

    private fun findLatestLogFile(dir: File, prefix: String): File? {
        if (!dir.exists()) return null
        return dir.listFiles { _, name -> name.startsWith("${prefix}_") && name.endsWith(".log") }
            ?.maxByOrNull { it.lastModified() }
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
