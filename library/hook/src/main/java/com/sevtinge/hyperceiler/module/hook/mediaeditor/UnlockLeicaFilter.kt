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
package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.utils.api.LazyClass.AndroidBuildCls
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import de.robv.android.xposed.*
import java.lang.reflect.*

object UnlockLeicaFilter : BaseHook() {
    private val leicaOld by lazy<List<Method>> {
        DexKit.findMemberList("UnlockLeicaFilterOld") { dexkit ->
            dexkit.findMethod {
                matcher {
                    // 仅适配 1.5 及 1.6 的部分版本，新版更换检测方式
                    declaredClass {
                        usingStrings("unSupportDeviceList", "stringResUrl")
                    }
                    modifiers = Modifier.FINAL
                    returnType = "boolean"
                    paramCount = 0
                }
            }
        }
    }
    private val leicaNew by lazy<Method> {
        DexKit.findMember("UnlockLeicaFilterNew") { dexkit ->
            dexkit.findMethod {
                matcher {
                    // declaredClass = "com.miui.mediaeditor.photo.filter.repository.FilterRepository" // 1.10.0.0.6 版本开始混淆，不再推荐使用
                    usingStrings("portrait_support_devices") // 临时解决方法，可能很快就会失效
                    returnType = "java.io.Serializable"
                }
            }.single()
        }
    }

    override fun init() {
        if (leicaOld.isNotEmpty()) {
            leicaOld.forEach { method ->
                logD(TAG, lpparam.packageName, "Old Leica name is $method") // debug 用
                method.createHook {
                    returnConstant(true)
                }
            }
        } else {
            logD(TAG, lpparam.packageName, "New Leica name is $leicaNew") // debug 用
            leicaNew.createHook {
                before {
                    XposedHelpers.setStaticObjectField(
                        AndroidBuildCls,
                        "DEVICE",
                        "aurora"
                    )
                }
            }
        }
    }
}
