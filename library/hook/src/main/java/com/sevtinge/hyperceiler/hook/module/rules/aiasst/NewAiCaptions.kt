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
package com.sevtinge.hyperceiler.hook.module.rules.aiasst

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method

object NewAiCaptions: BaseHook() {
    private val mSupportAiSubtitlesUtils by lazy {
        findClassIfExists("com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils")
    }

    private val getMethod by lazy<Method> {
        DexKit.findMember("AiCaptionsModel") {
           it.findMethod {
               matcher {
                   // SupportAiSubtitlesUtils
                   addEqString("SupportAiSubtitlesUtils")
                   addUsingField("Landroid/os/Build;->DEVICE:Ljava/lang/String;")

                   paramCount = 0
               }
           }.single()
        }
    }

    private val getMethodNew by lazy<Method> {
        DexKit.findMember("AiCaptionsModelNew") {
            it.findClass {
                matcher {
                    addEqString("SupportAiSubtitlesUtils")
                }
            }.findMethod {
                matcher {
                    // SupportAiSubtitlesUtils
                    addInvoke {
                        addUsingField("Landroid/os/Build;->DEVICE:Ljava/lang/String;")
                        paramCount = 0
                    }

                    addInvoke("Ljava/util/Set;->iterator()Ljava/util/Iterator;")
                }
            }.single()
        }
    }



    override fun init() {
        if (mSupportAiSubtitlesUtils == null) {
            runCatching {
                getMethod.createHook {
                    returnConstant(true)
                }
            }.onFailure {
                getMethodNew.createHook {
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
