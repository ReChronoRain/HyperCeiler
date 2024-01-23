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
package com.sevtinge.hyperceiler.module.hook.screenrecorder

import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SaveToMovies : BaseHook() {
    override fun init() {
        val clazz = XposedHelpers.findClass("android.os.Environment", lpparam.classLoader)
        XposedHelpers.setStaticObjectField(clazz, "DIRECTORY_DCIM", "Movies")

        XposedHelpers.findAndHookMethod("android.content.ContentValues",
            lpparam.classLoader,
            "put",
            String::class.java,
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == "relative_path") {
                        param.args[1] = (param.args[1] as String).replace("DCIM", "Movies")
                    }
                }
            })
    }
}
