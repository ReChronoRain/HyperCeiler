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
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getSerial
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.rootExecCmd
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 日志服务健康检查器
 */
object LoggerHealthChecker {
    @JvmField
    var LOGGER_CHECKER_ERR_CODE: String? = null

    private const val TAG = "HyperCeilerLogManager"
    private const val TIMEOUT_SECONDS = 5L

    @JvmStatic
    fun isLoggerAlive(): Boolean {
        return checkXposedLog() && checkLogcat()
    }

    private fun checkXposedLog(): Boolean {
        try {
            val lsposedLogDirExists = rootExecCmd("ls -d /data/adb/lspd/log/ 2>/dev/null")?.isNotEmpty() == true

            if (lsposedLogDirExists) {
                val latestLogFile = rootExecCmd("ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1")?.trim() ?: ""

                if (latestLogFile.isNotEmpty() && !latestLogFile.contains("No such file")) {
                    val grepOutput = rootExecCmd("grep -i -q 'HyperCeiler' $latestLogFile && echo 'FOUND' || echo 'EMPTY'")
                    if (grepOutput?.trim() == "EMPTY") {
                        LOGGER_CHECKER_ERR_CODE = "EMPTY_XPOSED_LOG_FILE"
                        return false
                    }
                } else {
                    LOGGER_CHECKER_ERR_CODE = "NO_XPOSED_LOG_FILE"
                    return false
                }
            }
        } catch (e: Exception) {
            LOGGER_CHECKER_ERR_CODE = "XPOSED_CHECK_ERROR: ${e.message}"
        }

        return true
    }

    private fun checkLogcat(): Boolean {
        val message = "LOGGER_ALIVE_SYMBOL_${getSerial()}"

        Log.d(TAG, message)

        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<Boolean> {
            var process: Process? = null
            try {
                // 使用 logcat -v threadtime 获取更详细的信息
                // 使用 -G 清空缓冲区后重新读取，确保能读到最新的日志
                process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "threadtime", "-s", "$TAG:D"))

                BufferedReader(InputStreamReader(process.inputStream)).use { bufferedReader ->
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        if (line?.contains(message) == true) {
                            LOGGER_CHECKER_ERR_CODE = "SUCCESS"
                            return@submit true
                        }
                    }
                }
                LOGGER_CHECKER_ERR_CODE = "NO_SUCH_LOG"
                false
            } catch (e: Exception) {
                LOGGER_CHECKER_ERR_CODE = "LOGCAT_ERROR: ${e.message}"
                false
            } finally {
                process?.destroy()
            }
        }

        return try {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            LOGGER_CHECKER_ERR_CODE = "TIME_OUT"
            future.cancel(true)
            false
        } catch (e: Exception) {
            LOGGER_CHECKER_ERR_CODE = "FUTURE_ERROR: ${e.message}"
            false
        } finally {
            executor.shutdownNow()
        }
    }

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
