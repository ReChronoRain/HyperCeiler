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
package com.sevtinge.hyperceiler.libhook.rules.camera

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockLeica : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        if (is60Camera) {
            unlockMethod1
        } else {
            unlockMethod2
        }
        unlockMethod3
        unlockMethod4
        return true
    }

    // 这破玩意写了十几个小时，得出的结论是，跨一个大版本就需要改一下特征点
    // 手上只有 5.3 和 6.1 两个版本，其他版本我不保证能解锁
    // 目前兼容到 6.4 版本
    // 累了，先这样吧
    // 后面如果能优化的话再说
    // 2025.5.1 ~ 2026.3.30
    val is60Camera by lazy {
        getPackageVersionCode(lpparam) >= 600000000
    }
    val is63Camera by lazy {
        getPackageVersionCode(lpparam) >= 630000000
    }
    val is64Camera by lazy {
        // 主包表示无法理解米米的行为，6.4 相机的内容和 6.2 一致，除了 CloudWatermark 这个云控内容
        getPackageVersionCode(lpparam) >= 640000000
    }

    private val unlockMethod1 by lazy<Method> {
        requiredMember("uM1") {
            // 6.x
            // 6.2 已合并方法，所以改回最初改颜色的方法
            it.findMethod {
                matcher {
                    declaredClass {
                        usingEqStrings("pref_qc_camera_sharpness_key", "pref_tint_color")
                    }

                    if (is63Camera && !is64Camera) {
                        // 相机 6.3.005670.0
                        addInvoke("Landroid/content/Context;->getColor(I)I")
                    } else {
                        addInvoke("Landroid/app/Application;->getColor(I)I")
                    }

                    modifiers = Modifier.STATIC or Modifier.PUBLIC
                    returnType = "int"
                }
                // 因为不知道为什么 DexKit 用 addCaller 筛不出 boolean E6.d.k1() 这种没有 modifiers 的方法，就只能这么写了
            }.single().invokes.last { get ->
                get.returnType?.name == "boolean" && get.paramCount == 0
            }
        }
    }

    private val unlockMethod2 by lazy<Method> {
        requiredMember("uM2") {
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

    private val unlockMethod3 by lazy<Method> {
        requiredMember("uM3") {
            if (is60Camera) {
                // 6.x
                it.findMethod {
                    matcher {
                        addCaller {
                            addAnnotation {
                                elementCount(3)
                                usingStrings("!isSupportThemeCV")
                            }

                            returnType = "java.util.List"
                        }

                        returnType = "boolean"
                        paramCount = 0
                    }
                }.single()
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
        requiredMemberList("uML4") {
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
        if (is60Camera) {
            unlockMethod1.createHook {
                returnConstant(true)
            }
        } else {
            unlockMethod2.createHook {
                returnConstant(0)
            }
        }

        unlockMethod3.createHook {
            returnConstant(true)
        }

        unlockMethod3.declaringClass.methodFinder()
            .filterByName(unlockMethod3.name.decrementLetters())
            .single().createHook {
                XposedLog.d(
                    TAG,
                    packageName,
                    "uM3: ${unlockMethod3.name}, uM3-1: ${unlockMethod3.name.decrementLetters()}"
                )
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

