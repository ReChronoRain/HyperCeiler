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
    private val isLeica = mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0) == 1
    private val isRedmi = mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0) == 2
    private val isPOCO = mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0) == 3

    override fun init() {
        // find 徕卡定制相框 && redmi 定制相框 && poco 定制相框 && 迪斯尼定制相框
        val publicA = dexKitBridge.findMethod {
            matcher {
                // 搜索符合条件的方法（1.6.0.0.5 举例，以下条件筛选完还有 a() c() e() g() h()）
                // g() 是 Redmi 中的 其中一个联名定制画框
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

        // 公共解锁特定机型定制相框使用限制
        val publicB = dexKitBridge.findMethod {
            matcher {
                // 定位指定类名
                // declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                // 搜索符合条件的方法（1.6.0.0.5 举例，以下条件筛选完还有 b(c cVar) d(c cVar) f(c cVar)）
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
                    usingStrings("getString(R.string.photo…allery_frame_device_only)")
                    modifiers = Modifier.FINAL
                    returnType("void")
                }
                modifiers = Modifier.FINAL
                returnType = "boolean"
                paramCount = 1
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()

        for (a in publicA) {
            logI(TAG,"Public A name is $a")
            when(a.name) {
                // 猫猫并不想这么搞，但是木得办法找出能让 dexKit 筛选的法子
                // 仅针对 1.6.0.0.5 版本进行适配，其他版本不保证能用
                "a" -> xiaomi(a)
                "c" -> poco(a)
                "e" -> redmi(a)
                else -> disney(a)
            }
        }

        // debug 用
        for (b in publicB) {
            logI(TAG,"Public B name is $b")
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

    private fun disney(name: Method) {
        name.createHook {
            returnConstant(true)
        }
    }
}
