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
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks

object DisableCleaner : BaseHook() {
    override fun init() {
        findClass("com.android.server.am.ActivityManagerService")
            .findAllMethods { name("checkExcessivePowerUsage") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.android.server.am.ActivityManagerShellCommand")
            .findAllMethods { name("runKillAll") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.android.server.am.CameraBooster")
            .findAllMethods { name("boostCameraIfNeeded") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.android.server.am.OomAdjuster")
            .findAllMethods { name("shouldKillExcessiveProcesses") }
            .createBeforeHooks {
                it.result = false
            }

        findClass("com.android.server.am.OomAdjuster")
            .findAllMethods { name("updateAndTrimProcessLSP") }
            .createBeforeHooks {
                it.args[2] = 0
            }

        findClass("com.android.server.am.PhantomProcessList")
            .findAllMethods { name("trimPhantomProcessesIfNecessary") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.android.server.am.ProcessMemoryCleaner")
            .findAllMethods { name("checkBackgroundProcCompact") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.android.server.am.ProcessPowerCleaner")
            .findAllMethods { name("handleAutoLockOff") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.android.server.am.SystemPressureController")
            .findAllMethods { name("nStartPressureMonitor") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.android.server.wm.RecentTasks")
            .findAllMethods { name("trimInactiveRecentTasks") }
            .createBeforeHooks {
                it.result = null
            }

        findClass("com.miui.cameraopt.adapter.ProcessManagerAdapter")
            .findAllMethods { name("killApplication") }
            .createBeforeHooks {
                it.result = null
            }

    }
}
