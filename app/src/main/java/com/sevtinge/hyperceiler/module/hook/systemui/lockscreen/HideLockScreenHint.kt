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
import com.github.kyuubiran.ezxhelper.ObjectUtils
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
            // 修复方案来自 Hyper Helper
            // 不知道为啥隐藏有点毛病，等修复
            hookAllConstructors(
                "com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController",
                lpparam.classLoader,
                object : MethodHook() {
                    @Throws(Throwable::class)
                    override fun after(param: MethodHookParam) {
                        XposedHelpers.setObjectField(param.thisObject, "mPersistentUnlockMessage", "")
                    }
                } )

            findAndHookMethod(
                "com.android.systemui.statusbar.KeyguardIndicationController",
                lpparam.classLoader,
                "setIndicationArea",
                object : MethodHook() {
                    @Throws(Throwable::class)
                    override fun after(param: MethodHookParam) {
                        val image = ObjectUtils.getObjectOrNullAs<ImageView>(param.thisObject, "mUpArrow")
                        image?.alpha = 0f
                    }
                })
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
