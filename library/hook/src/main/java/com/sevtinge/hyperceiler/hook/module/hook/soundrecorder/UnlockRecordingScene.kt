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
package com.sevtinge.hyperceiler.hook.module.hook.soundrecorder

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method

object UnlockRecordingScene : BaseHook() {
    private val unlockMethod by lazy<Method> {
        DexKit.findMember("recordScene") {
            it.findMethod {
                matcher {
                    usingEqStrings("support_record_param")
                    returnType = "boolean"
                }
            }.single()
        }
    }

    private val unlockMethod2 by lazy<Method> {
        DexKit.findMember("recordScene2") {
            it.findMethod {
                matcher {
                    usingEqStrings("support_hd_record_param")
                    returnType = "boolean"
                }
            }.single()
        }
    }

    override fun init() {
        unlockMethod.createHook {
            returnConstant(true)
        }

        unlockMethod2.createHook {
            returnConstant(true)
        }
    }
}
