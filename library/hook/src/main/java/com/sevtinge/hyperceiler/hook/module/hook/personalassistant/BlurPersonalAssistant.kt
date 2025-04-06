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
package com.sevtinge.hyperceiler.hook.module.hook.personalassistant

import android.graphics.drawable.*
import android.view.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.utils.api.getValueByFields
import java.lang.reflect.*
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

        DexKit.findMemberList<Method>("BlurPersonalAssistant") {
            it.findMethod {
                matcher {
                    usingEqStrings("ScrollStateManager")
                }
            }
        }.forEach { methodData ->
            methodData.createAfterHook {
                val scrollX = it.args[0] as Float
                val fieldNames = ('a'..'z').map { name -> name.toString() }
                val window = getValueByFields(it.thisObject, fieldNames) ?: return@createAfterHook

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
