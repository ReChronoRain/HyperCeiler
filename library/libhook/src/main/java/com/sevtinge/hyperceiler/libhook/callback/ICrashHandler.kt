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
package com.sevtinge.hyperceiler.libhook.callback

import android.app.ApplicationErrorReport
import android.content.Context

/**
 * 崩溃处理扩展接口
 */
interface ICrashHandler {
    /**
     * 当监测到目标应用发生崩溃，并且符合处理条件（如短时间内多次崩溃）时回调
     *
     * @param context 系统上下文
     * @param pkgName 崩溃的应用包名
     * @param crashInfo 崩溃详细信息 (堆栈等)
     * @param longMsg 详细错误信息
     * @param stackTrace 堆栈追踪字符串
     * @return true 表示已处理，false 表示未处理
     */
    fun onCrashDetected(
        context: Context,
        pkgName: String,
        crashInfo: ApplicationErrorReport.CrashInfo?,
        longMsg: String?,
        stackTrace: String?
    ): Boolean
}
