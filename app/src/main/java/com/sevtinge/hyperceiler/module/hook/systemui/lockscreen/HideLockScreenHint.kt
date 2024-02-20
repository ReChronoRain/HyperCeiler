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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.widget.ImageView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

object HideLockScreenHint : BaseHook() {
    override fun init() {
        val hook: MethodHook = object : MethodHook() {
            @Throws(Throwable::class)
            override fun before(param: MethodHookParam) {
                XposedHelpers.setObjectField(param.thisObject, "mUpArrowIndication", null)
            }
        }

        if (isAndroidVersion(34) && isMoreHyperOSVersion(1f)) {
            // by Hyper Helper
            loadClassOrNull("com.android.systemui.statusbar.KeyguardIndicationController")!!.methodFinder()
                .filterByName("updateDeviceEntryIndication")
                .single().createHook {
                    after {
                        XposedHelpers.setObjectField(it.thisObject, "mPersistentUnlockMessage", "")
                    }
                }

            loadClassOrNull("com.android.systemui.statusbar.KeyguardIndicationController")!!.methodFinder()
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
        } else {
            findAndHookMethod(
                "com.android.systemui.statusbar.KeyguardIndicationController",
                lpparam.classLoader,
                "updateIndication",
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                hook
            )
        }
    }
}
