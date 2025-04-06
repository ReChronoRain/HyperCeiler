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

package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.other

import android.app.Activity;
import android.os.Bundle;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

object RemoveSIMLockSuccessDialog : BaseHook() {
    @Throws(NoSuchMethodException::class)
    override fun init() {
        XposedHelpers.findAndHookMethod(
            "com.miui.simlock.activity.SuccessDialogActivity",
            lpparam.classLoader,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    (param.thisObject as Activity).finish()
                }
            }
        )
    }
}
