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
package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.api.*
import com.sevtinge.hyperceiler.utils.log.*
import java.lang.reflect.*

object UnlockCustomPhotoFrames : BaseHook() {
    private val frames by lazy {
        mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0)
    }
    private val isHookType by lazy {
        mPrefsMap.getStringAsInt("mediaeditor_hook_type", 0) == 2
    }
    private val isOpenSpring by lazy {
        mPrefsMap.getBoolean("mediaeditor_unlock_spring")
    }
    private val isLeica by lazy {
        if (!isHookType) {
            frames == 1
        } else {
            mPrefsMap.getBoolean("mediaeditor_unlock_custom_photo_frames_leica")
        }
    }
    private val isRedmi by lazy {
        if (!isHookType) {
            frames == 2
        } else {
            mPrefsMap.getBoolean("mediaeditor_unlock_custom_photo_frames_redmi")
        }
    }
    private val isPOCO by lazy {
        if (!isHookType) {
            frames == 3
        } else {
            mPrefsMap.getBoolean("mediaeditor_unlock_custom_photo_frames_poco")
        }
    }

    private val methodA by lazy<List<Method>> {
        DexKit.findMemberList("MA") { bridge ->
            bridge.findMethod {
                matcher {
                    // 改动日志:
                    // 现在这个查找方式直接兼容 1.5 - 1.9
                    // 1.6.5.10.2 之后迪斯尼定制画框解锁的地方和现在的不一样
                    // 1.7.5.0.4 之后，类名迁移，内部文件改动较大
                    // 合并缓存，现在只需查询一次即可获取全部 6 个需要 Hook 的方法，且不会出现多余的方法 (2025.1.8)
                    // 这里的 declaredClass 不清楚米米还会不会混淆，混淆了非常麻烦，比如从 1.3 - 1.4 版本的混淆
                    if (isHookType) {
                        declaredClass("com.miui.mediaeditor.config.galleryframe.GalleryFrameAccessUtils")
                    } else {
                        declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                    }
                    addUsingField {
                        // 如果后面有改的话请释放这个注释
                        // modifiers = Modifier.STATIC or Modifier.FINAL
                        type = "java.util.List"
                    }
                    returnType = "boolean"
                }
            }
        }
    }

    private val springA by lazy<Field> {
        DexKit.findMember("SA") { bridge ->
            bridge.findField {
                matcher {
                    // 改动日志:
                    // 这里是新春定制画框的解锁，仅在部分版本中存在
                    // 这里的 declaredClass 不清楚米米还会不会混淆，混淆了非常麻烦，比如从 1.3 - 1.4 版本的混淆
                    if (isHookType) {
                        declaredClass("com.miui.mediaeditor.config.galleryframe.GalleryFrameAccessUtils")
                    } else {
                        declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                    }
                    modifiers = Modifier.STATIC
                    type = "boolean"
                }
            }.single()
        }
    }

    private val redmiB by lazy<Field> {
        DexKit.findMember("RB") { bridge ->
            bridge.findField {
                matcher {
                    // 改动日志:
                    // 这里是红米定制联名画框的解锁
                    // 从 1.6.5.10.2 版本开始，解锁此画框的难度较高
                    // 这里的 declaredClass 不清楚米米还会不会混淆，混淆了非常麻烦，比如从 1.3 - 1.4 版本的混淆
                    if (isHookType) {
                        declaredClass("com.miui.mediaeditor.config.galleryframe.GalleryFrameAccessUtils")
                    } else {
                        declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                    }
                    modifiers = Modifier.STATIC or Modifier.FINAL
                }
            }.last()
        }
    }

    override fun init() {
        var index = 0
        val actions = listOf<(Method) -> Unit>(::xiaomi, ::poco, ::redmi)

        methodA.forEach { method ->
            if (method.paramCount == 1) {
                // 公共解锁特定机型定制画框使用限制
                other(method)
            } else {
                val action = actions[index]
                action(method)
                index = (index + 1) % actions.size
            }
        }

        if (isOpenSpring && springA != null) {
            springA.setBoolean(null, true)
        }

        /*if (isRedmi && redmiB != null) {

        }*/
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
