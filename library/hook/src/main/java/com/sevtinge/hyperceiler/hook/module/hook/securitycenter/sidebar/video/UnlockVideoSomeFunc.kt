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
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.sidebar.video

import com.sevtinge.hyperceiler.hook.module.base.*
import com.sevtinge.hyperceiler.hook.module.base.dexkit.*
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.enums.*
import java.lang.reflect.*

object UnlockVideoSomeFunc : BaseHook() {

    private val findFrcClass by lazy<Class<*>> {
        DexKit.findMember("findFrcClass") {
            it.findClass {
                matcher {
                    addUsingString("ro.vendor.media.video.frc.support", StringMatchType.Equals)
                }
            }.single()
        }
    }

    private val findFrc by lazy<List<Method>> {
        DexKit.findMemberList("findFrcA") {
            it.findMethod {
                matcher {
                    declaredClass = findFrcClass.name
                    returnType = "boolean"
                    paramTypes("java.lang.String")
                }
            }
        }
    }

    private val findTat by lazy<Method> {
        DexKit.findMember("findTat") {
            it.findMethod {
                matcher {
                    declaredClass = findFrcClass.name
                    addUsingString("debug.config.media.video.ais.support", StringMatchType.Equals)
                }
            }.single()
        }
    }

    private val memc by lazy {
        // 动态画面补偿
        mPrefsMap.getBoolean("security_center_unlock_memc")
    }
    private val enhance by lazy {
        // 影像轮廓增强
        mPrefsMap.getBoolean("security_center_unlock_enhance_contours")
    }
   private val resolution by lazy {
        // 极清播放
        mPrefsMap.getBoolean("security_center_unlock_s_resolution")
   }

    override fun init() {
        val ordered = DexKit.findMemberList<Method>("findFrcB") {
            it.findMethod {
                matcher {
                    declaredClass = findFrcClass.name
                    returnType = "boolean"
                    paramTypes("java.lang.String")
                }
            }.findMethod {
                matcher {
                    addUsingField {
                        type = "java.util.List"
                    }
                }
            }
        }
        val differentItems = findFrc.subtract(ordered.toSet())

        if (memc) {
            differentItems.forEach { methods ->
                logD(TAG, lpparam.packageName, "find MeMc Method is $methods")
                hook(methods)
            }
        }

        var counter = 0
        findFrc.forEach { methods ->
            counter++
            if ((resolution || enhance) && counter == 1) {
                logD(TAG, lpparam.packageName, "find Tat Method is $findTat")
            }

            if (counter == 1 && resolution) {
                logD(TAG, lpparam.packageName, "find SuperResolution Method is $methods")

                hook(methods)
                hook(findTat)
            } else if (counter == 3 && enhance) {
                hook(methods)

                val newChar = findTat.name.toCharArray()
                for (i in newChar.indices) {
                    newChar[i]++
                }
                val newName = String(newChar)
                logD(TAG, lpparam.packageName, "find EnhanceContours Method(${methods.declaringClass}) is $newName")
                findTat.declaringClass.methodFinder()
                    .filterByName(newName)
                    .first().createHook {
                        returnConstant(true)
                    }
            }
        }
    }

    private fun hook(methods: Method) {
        methods.createHook {
            returnConstant(true)
        }
    }
}
