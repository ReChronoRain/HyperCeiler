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
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import java.lang.reflect.Field
import java.lang.reflect.Method

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

    private val bothRecordMethod by lazy<Method> {
        DexKit.findMember("BothRecordMethod") {
            it.findMethod {
                matcher {
                    usingStrings("ro.vendor.audio.screenrecorder.bothrecord")
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

        findAndHookConstructor(getClass, object : IMethodHook {
            override fun after(param: AfterHookParam) {
                for (i in fieldData) {
                    param.thisObject.setObjectField(i.name, true)
                }
            } })

        hookMethod(bothRecordMethod, object : IMethodHook {
            override fun before(param: BeforeHookParam?) {
                param?.result = 1
            }
        })
    }
}
