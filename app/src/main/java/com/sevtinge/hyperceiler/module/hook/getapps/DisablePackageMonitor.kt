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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.getapps

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*

object DisablePackageMonitor : BaseHook() {

    override fun init() {
        // 使用root, adb, packageinstaller安装应用后, 应用商店有后台时会上传检查应用更新信息
        val initMethod = findClass("com.xiaomi.market.receiver.MyPackageMonitor").getMethod("init")
        initMethod.createHook {
            logD(TAG, lpparam.packageName, "FindAndHook 'init' method: $initMethod")
            replace { }
        }
    }

}
