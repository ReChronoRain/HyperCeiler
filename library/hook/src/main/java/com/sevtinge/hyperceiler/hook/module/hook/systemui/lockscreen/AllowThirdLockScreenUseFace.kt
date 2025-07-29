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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isAndroidVersion

object AllowThirdLockScreenUseFace : BaseHook() {
    override fun init() {
        loadClassOrNull("com.android.keyguard.KeyguardUpdateMonitor")?.methodFinder()
            ?.filterByName("isUnlockWithFacePossible")
            ?.single()?.createHook {
                returnConstant(true)
            }
        loadClassOrNull(if (isAndroidVersion(35)) "miui.stub.keyguard.KeyguardStub\$registerKeyguardUpdateMonitor\$1" else "miui.stub.MiuiStub\$3")?.methodFinder()
            ?.filterByName("isUnlockWithFingerprintPossible")
            ?.single()?.createHook {
                returnConstant(true)
            }
    }
}
