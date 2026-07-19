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
package com.sevtinge.hyperceiler.libhook.rules.screenrecorder

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setStaticObjectField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import java.util.Objects

object SaveToMovies : BaseHook() {
    private val clazz by lazy {
        loadClass("android.os.Environment", lpparam.classLoader)
    }

    override fun init() {
        clazz.setStaticObjectField("DIRECTORY_DCIM", "Movies")

        loadClass("android.content.ContentValues", lpparam.classLoader)
            .findMethod { name("put"); parameterTypes(String::class.java, String::class.java) }
            .createBeforeHook { param ->
                val param0 = param.args[0] as String

                if (Objects.equals(param0, "relative_path")) {
                    param.args[1] = (param.args[1] as String).replace("DCIM", "Movies")
                }
            }
    }
}
