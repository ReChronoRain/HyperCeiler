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
import com.sevtinge.hyperceiler.common.utils.ShellUtils.checkRootPermission
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

    const val ALIVE_THRESHOLD = 75

    // ============ 各项权重 ============
    private const val WEIGHT_LOGCAT = 30
    private const val WEIGHT_LSPD_DUMP = 40
    private const val WEIGHT_LSPD_PAGE_HEADER = 30
    private const val MIN_DUMP_FILE_SIZE_BYTES = 1L * 1024L

    private const val LOGCAT_READ_DELAY_MS = 150L
    private const val MAX_RETRIES = 3

    // 检查项标签：提取为常量，避免同一字符串字面量在文件内重复出现
    private const val TAG_OK = "OK"
    private const val TAG_NO_FILE = "NO_FILE"
    private const val TAG_NO_ROOT = "NO_ROOT"
    private val PAGE_MARKER_REGEX = Regex("^----part \\d+ start----$")

    /**
     * 执行所有检查，更新 [confidence] 和 [diagSummary]。
     * @return confidence >= [ALIVE_THRESHOLD]
     */
    @JvmStatic
    fun isLoggerAlive(): Boolean {
        var score = 0
        val diag = mutableListOf<String>()
        val hasRoot = checkRootPermission() == 0

        // Logcat 管道
        val logcatResult = checkLogcat()
        score += logcatResult.score
        diag.add("logcat=${logcatResult.tag}")

        var lspdDumpResult = CheckResult(0, TAG_NO_ROOT)
        var lspdHeaderResult = CheckResult(0, TAG_NO_ROOT)
        if (hasRoot) {
            lspdDumpResult = checkLspdDumpFiles()
            score += lspdDumpResult.score
            diag.add("lspd_dump=${lspdDumpResult.tag}")

            lspdHeaderResult = checkLspdPageHeaders()
            score += lspdHeaderResult.score
            diag.add("lspd_page=${lspdHeaderResult.tag}")
        } else {
            diag.add("lspd_dump=NO_ROOT")
            diag.add("lspd_page=NO_ROOT")
        }

        // LSPosed 转储文件缺失（文件日志未开启/尚未同步/root 不可用）不代表日志服务异常；
        // 只要 logcat 管道可用即视为可用（logcat-only 兜底，避免误报 UNHEALTHY 限制功能）。
        val lspdFilesUnavailable = !hasRoot ||
            (TAG_NO_FILE == lspdDumpResult.tag && TAG_NO_FILE == lspdHeaderResult.tag)
        if (TAG_OK == logcatResult.tag && lspdFilesUnavailable) {
            score = score.coerceAtLeast(ALIVE_THRESHOLD)
            diag.add("floor=LOGCAT_ONLY")
        }

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
     * 示例："HEALTHY(100%) logcat=OK lspd_dump=OK lspd_page=OK"
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
                    return CheckResult(WEIGHT_LOGCAT, TAG_OK)
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

    private fun checkLspdDumpFiles(): CheckResult {
        val dumpFiles = findCurrentLspdLogCandidates()

        if (dumpFiles.isEmpty()) {
            return CheckResult(0, TAG_NO_FILE)
        }
        if (dumpFiles.all { it.length() < MIN_DUMP_FILE_SIZE_BYTES }) {
            return CheckResult(WEIGHT_LSPD_DUMP / 4, "TOO_SMALL")
        }
        return CheckResult(WEIGHT_LSPD_DUMP, TAG_OK)
    }

    private fun checkLspdPageHeaders(): CheckResult {
        val dumpFiles = findCurrentLspdLogCandidates()
        if (dumpFiles.isEmpty()) {
            return CheckResult(0, TAG_NO_FILE)
        }
        val invalidFile = dumpFiles.firstOrNull { !hasValidPageMarkerHeader(it) }
        return if (invalidFile == null) {
            CheckResult(WEIGHT_LSPD_PAGE_HEADER, TAG_OK)
        } else {
            CheckResult(WEIGHT_LSPD_PAGE_HEADER / 2, "BAD_HEADER")
        }
    }

    private fun findCurrentLspdLogCandidates(): List<File> {
        val logBase = localLogBaseDir ?: return emptyList()
        return findLogCandidates(File(logBase, "lspd/log"))
    }

    private fun hasValidPageMarkerHeader(file: File): Boolean {
        return try {
            file.bufferedReader().use { reader ->
                val firstNonBlankLine = reader.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() }
                firstNonBlankLine != null && PAGE_MARKER_REGEX.matches(firstNonBlankLine)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun findLogCandidates(dir: File): List<File> {
        if (!dir.exists()) return emptyList()
        return dir.listFiles { _, name ->
            (name.startsWith("modules_") || name.startsWith("verbose_")) && name.endsWith(".log")
        }?.sortedBy { it.name } ?: emptyList()
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
