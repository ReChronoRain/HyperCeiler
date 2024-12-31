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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*

object HideLockScreenHint : BaseHook() {
    private val keyguardIndicationController by lazy {
        loadClassOrNull("com.android.systemui.statusbar.KeyguardIndicationController")
    }

    override fun init() {
        if (isAndroidVersion(35) && isMoreHyperOSVersion(2f)) {
            keyguardIndicationController!!.methodFinder()
                .filterByParamCount(1)
                .filterByParamTypes(keyguardIndicationController)
                .filterStatic().single().createHook {
                    returnConstant(null)
                }
        } else if (isAndroidVersion(34) && isMoreHyperOSVersion(1f)) {
            // by Hyper Helper
            keyguardIndicationController!!.methodFinder()
                .filterByName("updateDeviceEntryIndication")
                .single().createHook {
                    after {
                        XposedHelpers.setObjectField(it.thisObject, "mPersistentUnlockMessage", "")
                    }
                }

            keyguardIndicationController!!.methodFinder()
                .filterByName("setIndicationArea")
                .single().createHook {
                    after {
                        val image =
                            ObjectUtils.getObjectOrNullAs<ImageView>(it.thisObject, "mUpArrow") ?: return@after
                        image.alpha = 0.0f
                    }
                }
        } else if (isAndroidVersion(33)) {
            findAndHookMethod(
                "com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController",
                lpparam.classLoader,
                "hasIndicationsExceptResting",
                XC_MethodReplacement.returnConstant(true)
            )
        }
    }
}
