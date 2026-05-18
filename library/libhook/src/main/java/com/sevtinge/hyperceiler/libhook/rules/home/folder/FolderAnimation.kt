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
package com.sevtinge.hyperceiler.libhook.rules.home.folder

import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import kotlin.math.abs

class FolderAnimation : BaseHook() {
    companion object {
        private val sOverrideDampingResponse = ThreadLocal<FloatArray>()
    }

    var mLauncher: Class<*>? = null
    private var value1: Float? = null
    private var value2: Float? = null
    private var value3: Float? = null
    private var value4: Float? = null

    override fun init() {//|x-200|    50-150
        value1 = abs(PrefsBridge.getInt("home_folder_anim_1", 90).toFloat() - 200) / 100
        value2 = PrefsBridge.getInt("home_folder_anim_2", 30).toFloat() / 100
        value3 = abs(PrefsBridge.getInt("home_folder_anim_3", 99).toFloat() - 200) / 100
        value4 = PrefsBridge.getInt("home_folder_anim_4", 24).toFloat() / 100
        val mSpringAnimator = findClassIfExists("com.miui.home.launcher.animate.SpringAnimator")

        mSpringAnimator?.let {
            it.findMethod {
                name("setDampingResponse")
                parameterTypes(
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!
                )
            }.createHook {
                    before { param ->
                        val override = sOverrideDampingResponse.get() ?: return@before
                        param.args[0] = override[0]
                        param.args[1] = override[1]
                    }
                }
        }

        for (i in 47..60) {
            val launcherClass = findClassIfExists("com.miui.home.launcher.Launcher$$i")
            if (launcherClass != null) {
                for (field in launcherClass.declaredFields) {
                    if (field.name == $$"val$folderInfo") {
                        val mLauncherClass =
                            loadClassOrNull("com.miui.home.launcher.Launcher$$i") ?: continue

                        for (child in mLauncherClass.declaredFields) {
                            if (child.name != $$"val$folderInfo")
                                continue

                            mLauncherClass.findMethod {
                                name("run")
                                filter { parameterTypes.isEmpty() }
                            }.createHook {
                                    before {
                                        sOverrideDampingResponse.set(floatArrayOf(value1!!, value2!!))
                                    }
                                    after {
                                        sOverrideDampingResponse.remove()
                                    }
                                }
                            break
                        }
                    }
                }
            }
        }

        findClass("com.miui.home.launcher.Launcher").findMethod {
            name("closeFolder")
            parameterTypes(Boolean::class.javaPrimitiveType!!)
        }.createHook {
                before { param ->
                    if (param.args[0] == true) {
                        sOverrideDampingResponse.set(floatArrayOf(value3!!, value4!!))
                    }
                }
                after {
                    sOverrideDampingResponse.remove()
                }
            }

    }
}
