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
package com.sevtinge.hyperceiler.libhook.utils.api

import android.os.Handler
import android.os.Looper
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object HostExecutor {
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val taskMap = ConcurrentHashMap<Any, TaskToken>()
    @Volatile
    private var hotReloadCleanupRegistered = false

    private class TaskToken {
        @Volatile var future: Future<*>? = null
    }

    fun <T> execute(
        tag: Any,
        backgroundTask: () -> T?,
        runOnMain: Boolean = true,
        onResult: ((T) -> Unit)? = null
    ) {
        ensureHotReloadCleanup()
        val oldToken = taskMap[tag]
        oldToken?.future?.cancel(true)

        val newToken = TaskToken()
        taskMap[tag] = newToken

        val future = executor.submit {
            if (Thread.currentThread().isInterrupted) return@submit
            try {
                if (taskMap[tag] != newToken) return@submit
                val result = backgroundTask()
                if (result == null) {
                    taskMap.remove(tag, newToken)
                    return@submit
                }
                if (Thread.currentThread().isInterrupted) return@submit
                if (taskMap[tag] != newToken) return@submit
                if (onResult != null) {
                    if (runOnMain) {
                        mainHandler.post {
                            if (taskMap[tag] == newToken) {
                                onResult(result)
                                taskMap.remove(tag, newToken)
                            }
                        }
                        return@submit
                    } else {
                        onResult(result)
                    }
                }
                taskMap.remove(tag, newToken)
            } catch (_: InterruptedException) {
            } catch (e: Exception) {
                e.printStackTrace()
                taskMap.remove(tag, newToken)
            }
        }
        newToken.future = future
    }

    private fun ensureHotReloadCleanup() {
        if (hotReloadCleanupRegistered) return
        synchronized(this) {
            if (hotReloadCleanupRegistered) return
            BaseHook.registerHotReloadCleanup {
                taskMap.values.forEach { token -> token.future?.cancel(true) }
                taskMap.clear()
                mainHandler.removeCallbacksAndMessages(null)
                executor.shutdownNow()
                try {
                    if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                        throw IllegalStateException("HostExecutor did not stop before hot reload")
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IllegalStateException("Interrupted while stopping HostExecutor", e)
                }
                hotReloadCleanupRegistered = false
            }
            hotReloadCleanupRegistered = true
        }
    }
}
