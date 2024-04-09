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
package com.sevtinge.hyperceiler.module.hook.personalassistant

import android.graphics.drawable.*
import android.view.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.api.*
import kotlin.math.*

object BlurPersonalAssistant : BaseHook() {
    private val blurRadius by lazy {
        mPrefsMap.getInt("personal_assistant_blurradius", 80)
    }
    private val backgroundColor by lazy {
        mPrefsMap.getInt("personal_assistant_color", -1)
    }

    override fun init() {
        var lastBlurRadius = -1

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ScrollStateManager")
            }
        }.forEach { methodData ->
            methodData.getMethodInstance(lpparam.classLoader).createHook {
                after {
                val scrollX = it.args[0] as Float
                    val fieldNames = ('a'..'z').map { name -> name.toString() }
                    val window = BlurDraw.getValueByFields(it.thisObject, fieldNames) ?: return@after

                    if (window.javaClass.name.contains("Window")) {
                        runCatching {
                            window as Window
                            val blurRadius = (scrollX * blurRadius).toInt()
                            if (abs(blurRadius - lastBlurRadius) > 2) {
                                window.setBackgroundBlurRadius(blurRadius)
                                lastBlurRadius = blurRadius
                            }
                            val backgroundColorDrawable = ColorDrawable(backgroundColor)
                            backgroundColorDrawable.alpha = (scrollX * 255).toInt()
                            window.setBackgroundDrawable(backgroundColorDrawable)
                        }
                    }
                }
            }
        }
    }
}
