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
package com.sevtinge.hyperceiler.hook.module.hook.home.folder

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod
import de.robv.android.xposed.XC_MethodHook
import kotlin.math.abs

class FolderAnimation : BaseHook() {
    var mLauncher: Class<*>? = null
    private var value1: Float? = null
    private var value2: Float? = null
    private var value3: Float? = null
    private var value4: Float? = null

    override fun init() {//|x-200|    50-150
        value1 = abs(mPrefsMap.getInt("home_folder_anim_1", 90).toFloat() - 200) / 100
        value2 = mPrefsMap.getInt("home_folder_anim_2", 30).toFloat() / 100
        value3 = abs(mPrefsMap.getInt("home_folder_anim_3", 99).toFloat() - 200) / 100
        value4 = mPrefsMap.getInt("home_folder_anim_4", 24).toFloat() / 100
        val mSpringAnimator = findClassIfExists("com.miui.home.launcher.animate.SpringAnimator")
        var hook1: XC_MethodHook.Unhook? = null
        var hook2: XC_MethodHook.Unhook? = null

        for (i in 47..60) {
            val launcherClass = findClassIfExists("com.miui.home.launcher.Launcher$$i")
            if (launcherClass != null) {
                for (field in launcherClass.declaredFields) {
                    if (field.name == "val\$folderInfo") {
                        val mLauncherClass =
                            loadClassOrNull("com.miui.home.launcher.Launcher$$i") ?: continue

                        for (child in mLauncherClass.declaredFields) {
                            if (child.name != "val\$folderInfo")
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

        "com.miui.home.launcher.Launcher".hookBeforeMethod("closeFolder", Boolean::class.java) {
            if (it.args[0] == true) {
                hook2 = mSpringAnimator.hookBeforeMethod(
                    "setDampingResponse",
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType
                ) { hookParam ->
                    hookParam.args[0] = value3
                    hookParam.args[1] = value4
                }
            }
        }
        "com.miui.home.launcher.Launcher".hookAfterMethod("closeFolder", Boolean::class.java) {
            hook2?.unhook()
        }

    }
}
