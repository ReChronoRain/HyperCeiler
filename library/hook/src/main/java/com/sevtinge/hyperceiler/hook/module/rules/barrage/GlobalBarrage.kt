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
package com.sevtinge.hyperceiler.hook.module.rules.barrage

import android.content.ContentResolver
import android.provider.Settings
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks

// https://github.com/YifePlayte/WOMMO/blob/main/app/src/main/java/com/yifeplayte/wommo/hook/hooks/singlepackage/barrage/GlobalBarrage.kt
object GlobalBarrage : BaseHook() {

    override fun init() {
        loadClass($$"android.provider.Settings$Secure").methodFinder().filterByName("getInt")
            .toList().createHooks {
                after { param ->
                    if ((param.args[1] as String) == "gb_boosting" && param.result != 1) {
                        Settings.Secure.putInt(param.args[0] as ContentResolver?, "gb_boosting", 1)
                        param.result = 1
                    }
                }
            }
    }
}
