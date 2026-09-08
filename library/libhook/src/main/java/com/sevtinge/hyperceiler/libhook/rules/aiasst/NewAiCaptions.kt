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
package com.sevtinge.hyperceiler.libhook.rules.aiasst

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object NewAiCaptions : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        getMethodNewList
        if (getMethodNewList.isEmpty()) {
            getMethodNew
            if (getMethodNew == null) {
                getMethod
            }
        }
        return true
    }
    private val mSupportAiSubtitlesUtils by lazy {
        findClassIfExists("com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils")
    }

    private val getMethod by lazy<Method> {
        requiredMember("AiCaptionsModel") {
           it.findMethod {
               matcher {
                   // 5.8.0.20-
                   addEqString("SupportAiSubtitlesUtils")
                   addUsingField("Landroid/os/Build;->DEVICE:Ljava/lang/String;")

                   paramCount = 0
               }
           }.single()
        }
    }

    private val getMethodNew by lazy {
        optionalMember("AiCaptionsModelNew") {
            it.findClass {
                matcher {
                    addEqString("SupportAiSubtitlesUtils")
                }
            }.findMethod {
                matcher {
                    // 5.9.0.40+
                    addInvoke {
                        addUsingField("Landroid/os/Build;->DEVICE:Ljava/lang/String;")
                        paramCount = 0
                    }

                    addInvoke("Ljava/util/Set;->iterator()Ljava/util/Iterator;")
                }
            }.single()
        } as? Method
    }

    private val getMethodNewList by lazy<List<Method>> {
        optionalMemberList("AiCaptionsModelNewList") {
            it.findClass {
                matcher {
                    addEqString("SupportAiSubtitlesUtils")
                }
            }.findMethod {
                matcher {
                    // 5.12.1.40 (实际应在 5.11.0.40+ 就改了)
                    // public static java.lang.Boolean m5.a2.m()
                    // public static java.lang.Boolean m5.a2.r()
                    addInvoke {
                        addInvoke {
                            addUsingField("Landroid/os/Build;->DEVICE:Ljava/lang/String;")
                            paramCount = 0
                        }
                        modifiers = Modifier.PUBLIC
                        paramCount = 1
                    }
                    modifiers = Modifier.PUBLIC
                    returnType = "java.lang.Boolean"
                }
            }
        }
    }

    val is1140AICaptions by lazy {
        getPackageVersionCode(lpparam) >= 540110140
    }

    override fun init() {
        if (is1140AICaptions) {
            getMethodNewList.createHooks {
                returnConstant(true)
            }
        } else if (mSupportAiSubtitlesUtils == null) {
            runCatching {
                getMethod.createHook {
                    returnConstant(true)
                }
            }.onFailure {
                getMethodNew?.createHook {
                    returnConstant(true)
                }
            }
        } else {
            runCatching {
                mSupportAiSubtitlesUtils.methodFinder()
                    .filterByName("isSupportAiSubtitles")
                    .single().createHook {
                        returnConstant(true)
                    }

                mSupportAiSubtitlesUtils.methodFinder()
                    .filterByName("isSupportOfflineAiSubtitles")
                    .single().createHook {
                        returnConstant(true)
                    }

            }
        }
    }
}

