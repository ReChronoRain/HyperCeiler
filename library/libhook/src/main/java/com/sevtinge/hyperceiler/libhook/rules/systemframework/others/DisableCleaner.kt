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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks

object DisableCleaner : BaseHook() {
    override fun init() {
        hookBefore("com.android.server.am.ActivityManagerService", "checkExcessivePowerUsage") {
            it.result = null
        }

        hookBefore("com.android.server.am.ActivityManagerShellCommand", "runKillAll") {
            it.result = null
        }

        hookBefore("com.android.server.am.CameraBooster", "boostCameraIfNeeded") {
            it.result = null
        }

        hookBefore("com.android.server.am.OomAdjuster", "shouldKillExcessiveProcesses") {
            it.result = false
        }

        hookBefore("com.android.server.am.OomAdjuster", "updateAndTrimProcessLSP") {
            it.args[2] = 0
        }

        hookBefore("com.android.server.am.PhantomProcessList", "trimPhantomProcessesIfNecessary") {
            it.result = null
        }

        hookBefore("com.android.server.am.ProcessMemoryCleaner", "checkBackgroundProcCompact") {
            it.result = null
        }

        hookBefore("com.android.server.am.ProcessPowerCleaner", "handleAutoLockOff") {
            it.result = null
        }

        hookBefore("com.android.server.am.SystemPressureController", "nStartPressureMonitor") {
            it.result = null
        }

        hookBefore("com.android.server.wm.RecentTasks", "trimInactiveRecentTasks") {
            it.result = null
        }

        hookBefore("com.miui.cameraopt.adapter.ProcessManagerAdapter", "killApplication") {
            it.result = null
        }
    }

    // Not every ROM ships all of these classes and methods, so a missing
    // target must only skip its own hook instead of aborting the rest.
    private fun hookBefore(
        className: String,
        methodName: String,
        callback: (HookParam) -> Unit
    ) {
        val clazz = findClassIfExists(className)
        if (clazz == null) {
            XposedLog.w(TAG, packageName, "Skip $className.$methodName: class not found")
            return
        }

        try {
            val methods = clazz.findAllMethods { name(methodName) }
            if (methods.isEmpty()) {
                XposedLog.w(TAG, packageName, "Skip $className.$methodName: method not found")
                return
            }
            methods.createBeforeHooks(callback = callback)
        } catch (t: Throwable) {
            XposedLog.e(TAG, packageName, "Hook Failed in $className.$methodName: ", t)
        }
    }
}
