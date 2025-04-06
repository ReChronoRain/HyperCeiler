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
package com.sevtinge.hyperceiler.hook.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.utils.api.LazyClass.AndroidBuildCls
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

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
            // 改动日志:
            // 现在这个查找方式直接兼容 1.5 - 1.10
            // 1.6.5.10.2 之后迪斯尼定制画框解锁的地方和现在的不一样
            // 1.7.5.0.4 之后，类名迁移，内部文件改动较大
            // 合并缓存，现在只需查询一次即可获取全部 6 个需要 Hook 的方法，且不会出现多余的方法 (2025.1.8)
            // 1.10.0.0.6 之后，类名混淆
            // 所以，它要是再把特征混淆完了的话，那 886 了
            // 这构思玩意都快接近 hook 安全服务了 (2025.3.19)
            if (isHookType) {
                // 使用二次查询，虽然慢，但是能找全
                bridge.findMethod {
                    matcher {
                        addCaller {
                            // paramCount = 2
                            returnType = "java.util.LinkedHashMap"
                            modifiers = Modifier.STATIC or Modifier.FINAL
                        }
                        addUsingField {
                            type = "java.util.List"
                        }
                        returnType = "boolean"
                    }
                }.last().declaredClass?.findMethod {
                    matcher {
                        addUsingField {
                            type = "java.util.List"
                        }
                        returnType = "boolean"
                    }
                }
            } else {
                bridge.findMethod {
                    matcher {
                        declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                        addUsingField {
                            type = "java.util.List"
                        }
                        returnType = "boolean"
                    }
                }
            }
        }
    }

    private val methodB by lazy<Method?> {
        DexKit.findMember("MB") { bridge ->
            bridge.findMethod {
                matcher {
                    paramCount = 2
                    returnType = "java.util.LinkedHashMap"
                    modifiers = Modifier.STATIC or Modifier.FINAL
                }
            }.single()
        }
    }

    private val springA by lazy<Field?> {
        DexKit.findMember("SA") { bridge ->
            bridge.findField {
                matcher {
                    // 改动日志:
                    // 这里是新春定制画框的解锁，仅在部分版本中存在
                    // 1.10.0.0.6 之后，类名混淆，只能获取 methodA 所属的 Class
                    if (isHookType) {
                        // declaredClass("com.miui.mediaeditor.config.galleryframe.GalleryFrameAccessUtils")
                        declaredClass(methodA.first().declaringClass.name)
                    } else {
                        declaredClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils")
                    }
                    modifiers = Modifier.STATIC
                    type = "boolean"
                }
            }.single()
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
            springA!!.setBoolean(null, true)
        }

        if (isLeica && methodB != null) {
            // 1.10.0.0.6 新增 Xiaomi 15 Ultra 独占定制画框
            methodB?.createBeforeHook {
                XposedHelpers.setStaticObjectField(
                    AndroidBuildCls,
                    "DEVICE",
                    "xuanyuan"
                )
            }
        }

        if (isRedmi) {
            // Redmi Note 13 Pro+ 定制版画框
            runCatching {
                loadClass("${methodA.first().declaringClass.name}\$a").methodFinder()
                    .filterByName("invoke").first()
                    .createHook {
                        returnConstant(true)
                    }
            }.recoverCatching {
                loadClass("com.miui.mediaeditor.config.galleryframe.GalleryFrameAccessUtils\$isVictoriaDeviceSupport$2").methodFinder()
                    .filterByName("invoke").first()
                    .createHook {
                        returnConstant(true)
                    }
            }.onFailure {
                loadClass("com.miui.mediaeditor.photo.config.galleryframe.GalleryFrameAccessUtils\$isVictoriaDeviceSupport$2").methodFinder()
                    .filterByName("invoke").first()
                    .createHook {
                        returnConstant(true)
                    }
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
