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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import kotlin.math.abs

class FolderAnimation : BaseHook() {
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
        var hook1: MethodUnhooker<*>? = null
        var hook2: MethodUnhooker<*>? = null

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

                            mLauncherClass.methodFinder()
                                .filterByName("run")
                                .first().createHook {
                                    before {
                                        hook1 = mSpringAnimator.methodFinder()
                                            .filterByName("setDampingResponse")
                                            .filterByParamTypes {
                                                it[0] == Float::class.javaPrimitiveType &&
                                                    it[1] == Float::class.javaPrimitiveType
                                            }.single().createHook {
                                                before {
                                                    it.args[0] = value1
                                                    it.args[1] = value2
                                                }
                                            }
                                    }
                                    after {
                                        hook1?.unhook()
                                    }
                                }
                            break
                        }
                    }
                }
            }
        }

        findClass("com.miui.home.launcher.Launcher").beforeHookMethod("closeFolder", Boolean::class.java) {
            if (it.args[0] == true) {
                hook2 = mSpringAnimator.beforeHookMethod(
                    "setDampingResponse",
                    Float::class.java,
                    Float::class.java
                ) { hookParam ->
                    hookParam.args[0] = value3
                    hookParam.args[1] = value4
                }
            }
        }
        findClass("com.miui.home.launcher.Launcher").afterHookMethod("closeFolder", Boolean::class.java) {
            hook2?.unhook()
        }

    }
}
