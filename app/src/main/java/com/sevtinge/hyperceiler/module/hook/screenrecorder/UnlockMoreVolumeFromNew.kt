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
package com.sevtinge.hyperceiler.module.hook.screenrecorder

import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import de.robv.android.xposed.*
import java.lang.reflect.*

object UnlockMoreVolumeFromNew : BaseHook() {
    private val getClass by lazy<Class<*>> {
        DexKit.findMember("UnlockMoreVolumeFromNewClass") {
            it.findClass {
                matcher {
                    usingEqStrings("support_a2dp_inner_record")
                }
            }.single()
        }
    }

    override fun init() {
        val fieldData = DexKit.findMemberList<Field>("UnlockMoreVolumeFromNewField") { dexkit ->
            dexkit.findField {
                matcher {
                    declaredClass(getClass)
                    type = "boolean"
                }
            }
        }

        findAndHookConstructor(getClass, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                for (i in fieldData) {
                    XposedHelpers.setObjectField(param.thisObject, i.name, true)
                }
            } })
    }
}
