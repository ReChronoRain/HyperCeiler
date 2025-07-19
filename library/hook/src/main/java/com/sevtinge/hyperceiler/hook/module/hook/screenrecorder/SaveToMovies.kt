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
package com.sevtinge.hyperceiler.hook.module.hook.screenrecorder

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.setStaticObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import java.util.*
import kotlin.getValue

object SaveToMovies : BaseHook() {
    private val clazz by lazy {
        loadClass("android.os.Environment", lpparam.classLoader)
    }

    override fun init() {
        clazz.setStaticObjectField("DIRECTORY_DCIM", "Movies")

        loadClass("android.content.ContentValues", lpparam.classLoader)
            .methodFinder().filterByName("put")
            .filterByParamTypes {
                it[0] == String::class.java && it[1] == String::class.java
            }.single().createBeforeHook { param ->
                val param0 = param.args[0] as String

                if (Objects.equals(param0, "relative_path")) {
                    param.args[1] = (param.args[1] as String).replace("DCIM", "Movies")
                }
            }
    }
}
