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
package com.sevtinge.hyperceiler.module.hook.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.findClassOrNull
import com.sevtinge.hyperceiler.utils.setBooleanField

object DisableSafeModelTip : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("android.provider.MiuiSettings\$Ad")
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }

        // 屏蔽每 30 天提示开启安全守护的弹窗（已知问题：完成和打开按钮无反应）
        /*val result2 = Objects.requireNonNull(
            mPackageInstallerResultMethodsMap!!["Disable30DaysDialog"]
        )

        for (descriptor in result2) {
            val mDisableSafeModelTip = descriptor.getMethodInstance(lpparam.classLoader)
            mDisableSafeModelTip.createHook {
                returnConstant(null)
            }
        }*/

        var letter = 'a'
        for (i in 0..25) {
            try {
                val classIfExists =
                    "com.miui.packageInstaller.ui.listcomponets.${letter}0".findClassOrNull()
                classIfExists?.let {
                    it.methodFinder().filterByName("a").first().createHook {
                        after { hookParam ->
                            try {
                                hookParam.thisObject.setBooleanField("m", false)
                            } catch (_: Throwable) {
                                hookParam.thisObject.setBooleanField("l", false)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                letter++
            }
        }
    }
}
