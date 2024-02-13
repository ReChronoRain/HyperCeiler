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
package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockCustomPhotoFrames : BaseHook() {
    private val frames by lazy {
        mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0)
    }
    private val isOpenSpring by lazy {
        mPrefsMap.getBoolean("mediaeditor_unlock_spring")
    }
    private val isLeica by lazy { frames == 1 }
    private val isRedmi by lazy { frames == 2 }
    private val isPOCO by lazy { frames == 3 }

    private val publicAClass by lazy {
        dexKitBridge.findMethod {
            matcher {
                // find 徕卡定制画框 && redmi 定制画框 && poco 定制画框 && 迪斯尼定制画框 && 新春定制画框
                // 搜索符合条件的方法（1.6.3.5 举例，以下条件筛选完还有 a() b() d() f() h() i()）
                // b() 是新春定制画框，前置条件需要符合定制画框类型(徕卡定制画框 或 redmi 定制画框)
                // h() 是 Redmi 中的 其中一个联名定制画框
                // 如果都返回 true 的话，按照原代码逻辑，只会解锁徕卡定制画框
                addCall {
                    declaredClass {
                        modifiers = Modifier.FINAL or Modifier.PUBLIC
                    }
                    modifiers = Modifier.FINAL or Modifier.STATIC or Modifier.PUBLIC
                    // paramCount = 2
                    returnType("java.util.List")
                }
                modifiers = Modifier.FINAL or Modifier.STATIC
                returnType = "boolean"
                paramCount = 0
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    private val publicA by lazy {
        dexKitBridge.findMethod {
            matcher {
                // 搜索符合条件的方法（1.6.3.5 举例，以下条件筛选完还有 a() d() f() h()）
                addCall {
                    declaredClass {
                        modifiers = Modifier.FINAL or Modifier.PUBLIC
                    }
                    modifiers = Modifier.FINAL or Modifier.STATIC or Modifier.PUBLIC
                    returnType("java.util.List")
                }
                modifiers = Modifier.FINAL or Modifier.STATIC
                returnType = "boolean"
                paramCount = 0

                addUsingField {
                    modifiers = Modifier.STATIC or Modifier.FINAL
                }
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    // 公共解锁特定机型定制画框使用限制
    private val publicB by lazy {
        dexKitBridge.findMethod {
            matcher {
                // 定位指定类名
                // declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                // 搜索符合条件的方法（1.6.3.5 举例，以下条件筛选完还有 c(c cVar) e(c cVar) g(c cVar)）
                addCall {
                    // 1.6 用的匹配
                    declaredClass {
                        addUsingStringsEquals("appContext()", "OffsetTime")
                    }
                    modifiers = Modifier.FINAL
                    returnType = "boolean"
                    paramCount = 1
                }
                modifiers = Modifier.FINAL
                returnType = "boolean"
                paramCount = 1
            }
            matcher {
                addCall {
                    // 1.5 用的匹配
                    addUsingStringsEquals("getString(R.string.photo…allery_frame_device_only)")
                    modifiers = Modifier.FINAL
                    returnType("void")
                }
                modifiers = Modifier.FINAL
                returnType = "boolean"
                paramCount = 1
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    override fun init() {
        val actions = listOf<(Method) -> Unit>(::xiaomi, ::poco, ::redmi, ::other)
        val orderedPublicAp = publicAClass.toList()
        val orderedPublicA = publicA.toList()
        val differentItems = orderedPublicAp.subtract(orderedPublicA.toSet())
        var index = 0

        if (isOpenSpring) {
            differentItems.forEach { method ->
                // debug 用
                logI(TAG, "Public Spring name is $method")
                other(method)  // 1.6.0.5.2 新增限时新春定制画框
            }
        }

        for (a in orderedPublicA) {
            // debug 用
            logI(TAG, "Public A name is $a")

            val action = actions.getOrElse(index) { ::other }
            action(a)
            index = (index + 1) % actions.size
        }

        // debug 用
        for (b in publicB) {
            logI(TAG, "Public B name is $b")
        }
        publicB.createHooks {
            returnConstant(true)
        }
    }

    private fun xiaomi(name: Method) {
        name.createHook {
            returnConstant(isLeica)
        }
    }

    private fun redmi(name: Method) {
        name.createHook {
            returnConstant(isRedmi)
        }
    }

    private fun poco(name: Method) {
        name.createHook {
            returnConstant(isPOCO)
        }
    }

    private fun other(name: Method) {
        name.createHook {
            returnConstant(true)
        }
    }
}
