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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemframework

import com.sevtinge.hyperceiler.module.base.BaseHook

object DisableCleaner : BaseHook() {
    override fun init() {
        hookAllMethods("com.android.server.am.ActivityManagerService", "checkExcessivePowerUsage",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.android.server.am.ActivityManagerShellCommand", "runKillAll",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.android.server.am.CameraBooster", "boostCameraIfNeeded",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.android.server.am.OomAdjuster", "shouldKillExcessiveProcesses",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = false
                }
            }
        )
        hookAllMethods("com.android.server.am.OomAdjuster", "updateAndTrimProcessLSP",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.args[2] = 0
                }
            }
        )
        hookAllMethods("com.android.server.am.PhantomProcessList", "trimPhantomProcessesIfNecessary",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.android.server.am.ProcessMemoryCleaner", "checkBackgroundProcCompact",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.android.server.am.ProcessPowerCleaner", "handleAutoLockOff",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.android.server.am.SystemPressureController", "nStartPressureMonitor",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.android.server.wm.RecentTasks", "trimInactiveRecentTasks",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
        hookAllMethods("com.miui.cameraopt.adapter.ProcessManagerAdapter", "killApplication",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
    }
}
