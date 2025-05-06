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
package com.sevtinge.hyperceiler.hook.module.hook.camera

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionCode
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockLeica : BaseHook() {
    // 这破玩意写了十几个小时，得出的结论是，跨一个大版本就需要改一下特征点
    // 手上只有 5.3 和 6.1 两个版本，其他版本我不保证能解锁
    // 累了，先这样吧
    // 后面如果能优化的话再说
    // 2025.5.1
    val isNewCamera by lazy {
        getPackageVersionCode(lpparam) >= 600000000
    }

    // 想单独开启 6.x 的红色就这么匹配
    private val unlockMethod1 by lazy<Method> {
        DexKit.findMember("uM1") {
            it.findMethod {
                matcher {
                    declaredClass {
                        usingEqStrings("pref_qc_camera_sharpness_key", "pref_tint_color")
                    }
                    addInvoke("Landroid/app/Application;->getColor(I)I")

                    modifiers = Modifier.STATIC or Modifier.PUBLIC
                    returnType = "int"
                }
            }.single().invokes.single { methodData ->
                methodData.returnType?.name == "boolean" && methodData.paramCount == 0
            }
        }
    }

    private val unlockMethod2 by lazy<Method> {
        DexKit.findMember("uM2") {
            if (isNewCamera) {
                // 6.x
                it.findMethod {
                    matcher {
                        declaredClass {
                            usingEqStrings(
                                "WatermarkTypePreference",
                                "watermark_westcoast3_evil_queen"
                            )
                        }

                        modifiers = Modifier.FINAL
                        returnType = "void"
                        usingStrings("updateViewCV")
                    }
                }.single().invokes.single { methodData ->
                    methodData.returnType?.name == "int" && methodData.paramCount == 0
                }
            } else {
                // 5.x
                it.findMethod {
                    matcher {
                        addCaller {
                            addCaller {
                                declaredClass {
                                    usingStrings("themeCustomize")
                                }

                                modifiers = Modifier.STATIC
                                paramCount = 1
                                returnType = "void"
                            }
                            returnType = "boolean"
                        }
                        returnType = "int"
                    }
                }.last()
            }
        }
    }

    private val unlockMethod3 by lazy<Method> {
        DexKit.findMember("uM3") {
            if (isNewCamera) {
                // 6.x
                it.findMethod {
                    matcher {
                        addAnnotation {
                            elementCount(3)
                            usingStrings("!isSupportThemeCV")
                        }

                        returnType = "java.util.List"
                    }
                }.single().invokes.single { methodData ->
                    methodData.returnType?.name == "boolean" && methodData.paramCount == 0
                }
            } else {
                // 5.x
                it.findMethod {
                    matcher {
                        addCaller {
                            addCaller {
                                addCaller {
                                    declaredClass {
                                        usingStrings("themeCustomize")
                                    }

                                    modifiers = Modifier.STATIC
                                    paramCount = 1
                                    returnType = "void"
                                }
                                returnType = "boolean"
                            }
                            returnType = "boolean"
                        }
                    }
                }.single()
            }
        }
    }

    private val unlockMethod4 by lazy<List<Method>> {
        // 你家最缺德的 ku 就要查重写方法了
        DexKit.findMemberList("uML4") {
            it.findMethod {
                matcher {
                    declaredClass {
                        superClass = unlockMethod3.declaringClass.name
                    }
                    name = unlockMethod3.name
                }
            }
        }
    }

    override fun init() {
        if (isNewCamera) {
            unlockMethod1.createHook {
                returnConstant(true)
            }
        }

        unlockMethod2.createHook {
            returnConstant(
               if (isNewCamera) 4 else 0
            )
        }

        unlockMethod3.createHook {
            returnConstant(true)
        }

        unlockMethod3.declaringClass.methodFinder()
            .filterByName(unlockMethod3.name.decrementLetters())
            .single().createHook {
                logD(TAG, lpparam.packageName, "uM3: ${unlockMethod3.name}, uM3-1: ${unlockMethod3.name.decrementLetters()}")
                returnConstant(true)
            }

        unlockMethod4.createHooks {
            returnConstant(true)
        }
    }

    private fun String.decrementLetters() = this.map { c ->
        if (c.isLetter()) (c.code - 1).toChar() else c
    }.joinToString("")
}