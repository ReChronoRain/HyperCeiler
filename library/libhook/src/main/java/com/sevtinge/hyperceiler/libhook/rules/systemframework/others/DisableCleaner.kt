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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods

object DisableCleaner : BaseHook() {
    override fun init() {
        findClass("com.android.server.am.ActivityManagerService")
            .hookAllMethods("checkExcessivePowerUsage") {
                before {
                    it.result = null
                }
            }

        findClass("com.android.server.am.ActivityManagerShellCommand")
            .hookAllMethods("runKillAll") {
                before {
                    it.result = null
                }
            }

        findClass("com.android.server.am.CameraBooster")
            .hookAllMethods("boostCameraIfNeeded") {
                before {
                    it.result = null
                }
            }

        findClass("com.android.server.am.OomAdjuster")
            .hookAllMethods("shouldKillExcessiveProcesses") {
                before {
                    it.result = false
                }
            }

        findClass("com.android.server.am.OomAdjuster")
            .hookAllMethods("updateAndTrimProcessLSP") {
                before {
                    it.args[2] = 0
                }
            }

        findClass("com.android.server.am.PhantomProcessList")
            .hookAllMethods("trimPhantomProcessesIfNecessary") {
                before {
                    it.result = null
                }
            }

        findClass("com.android.server.am.ProcessMemoryCleaner")
            .hookAllMethods("checkBackgroundProcCompact") {
                before {
                    it.result = null
                }
            }

        findClass("com.android.server.am.ProcessPowerCleaner")
            .hookAllMethods("handleAutoLockOff") {
                before {
                    it.result = null
                }
            }

        findClass("com.android.server.am.SystemPressureController")
            .hookAllMethods("nStartPressureMonitor") {
                before {
                    it.result = null
                }
            }

        findClass("com.android.server.wm.RecentTasks")
            .hookAllMethods("trimInactiveRecentTasks") {
                before {
                    it.result = null
                }
            }

        findClass("com.miui.cameraopt.adapter.ProcessManagerAdapter")
            .hookAllMethods("killApplication") {
                before {
                    it.result = null
                }
            }

    }
}
