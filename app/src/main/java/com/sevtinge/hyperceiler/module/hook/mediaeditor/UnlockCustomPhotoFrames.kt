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

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.*
import java.lang.reflect.*

object UnlockCustomPhotoFrames : BaseHook() {
    private val frames by lazy {
        mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0)
    }
    private val isOpenSpring by lazy {
        mPrefsMap.getBoolean("mediaeditor_unlock_spring")
    }
    private val isNewMediaeditor by lazy {
        // 以 1.7.5.0.4 为新旧版本分界线
        getPackageVersionCode(lpparam) >= 4658180
    }
    private val isLeica by lazy { frames == 1 }
    private val isRedmi by lazy { frames == 2 }
    private val isPOCO by lazy { frames == 3 }

    private val publicA by lazy<List<Method>> {
        DexKit.findMemberList("PA") { bridge ->
            bridge.findMethod {
                matcher {
                    // 真是妹想到啊，1.5 和 1.6 版本还以为不会套回去了
                    // 现在这个查找方式直接兼容 1.4 - 1.9
                    // 1.6.5.10.2 迪斯尼定制画框解锁的地方和现在的不一样
                    // 1.7.5.0.4 之后，类名迁移，内部文件改动较大，此为临时解决方案
                    if (isNewMediaeditor) {
                        declaredClass("com.miui.mediaeditor.config.galleryframe.GalleryFrameAccessUtils")
                    } else {
                        declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                    }
                    modifiers = Modifier.FINAL or Modifier.STATIC
                    returnType = "boolean"
                    paramCount = 0
                    // 1.6.5.10.2 以上方法查找完剩下 a() c() e()
                }
            }
        }
    }

    // 公共解锁特定机型定制画框使用限制
    private val publicB by lazy<List<Method>> {
        DexKit.findMemberList("PB") { dexkit ->
            dexkit.findMethod {
                matcher {
                    if (isNewMediaeditor) {
                        declaredClass("com.miui.mediaeditor.config.galleryframe.GalleryFrameAccessUtils")
                    } else {
                        declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                    }
                    // modifiers = Modifier.STATIC // 1.6.5.10.2 改成 STATIC，原来是 FINAL
                    returnType = "boolean"
                    paramCount = 1
                    addUsingField {
                        modifiers = Modifier.STATIC or Modifier.FINAL
                    }
                }
            }
        }
    }

    override fun init() {
        val publicC = DexKit.findMemberList<Method>("PC") {
            it.findMethod {
                matcher {
                    publicA.forEach { a ->
                        name = a.name
                    }
                    usingFields {
                        add {
                            type = "boolean"
                        }
                    }
                }
            }
        }

        val actions = if (isNewMediaeditor) {
            listOf<(Method) -> Unit>(::xiaomi, ::redmi, ::poco, ::other)
        } else {
            listOf<(Method) -> Unit>(::xiaomi, ::poco, ::redmi, ::other)
        }
        val differentItems = publicA.subtract(publicC.toSet())
        var index = 0

        differentItems.forEach { method ->
            val action = actions.getOrElse(index) { ::other }
            action(method)
            index = (index + 1) % actions.size
        }

        publicB.forEach { method ->
            other(method)
        }

        if (isOpenSpring && publicC.isNotEmpty()) {
            publicC.forEach { method ->
                other(method)
                // 1.6.0.5.2 新增限时新春定制画框
                // 后续版本已移除，其实可以删掉的，但还是留着吧，兴许后面可能还有用
            }
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
